package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.ARGB;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** 将现有物品模型以独立的半透明提交节点绘制，避免污染全局渲染状态。 */
@Environment(EnvType.CLIENT)
final class TranslucentItemPreviewRenderer {
    private static final ThreadLocal<PreviewCollector> PREVIEW_COLLECTOR =
            ThreadLocal.withInitial(PreviewCollector::new);

    static void submit(ItemStackRenderState model, PoseStack poses, SubmitNodeCollector collector,
                       int light, int overlay, float alpha) {
        if (alpha <= 0.0F || model.isEmpty()) return;
        PreviewCollector preview = PREVIEW_COLLECTOR.get();
        preview.begin(collector, ARGB.white(alpha));
        try {
            model.submit(poses, preview.proxy(), light, overlay, 0);
        } finally {
            preview.end();
        }
    }

    private static void submitQuads(SubmitNodeCollector collector, PoseStack poses, int light, int overlay,
                                    int[] tints, List<BakedQuad> quads, int previewColor) {
        List<BakedQuad> blockAtlas = new ArrayList<>();
        List<BakedQuad> itemAtlas = new ArrayList<>();
        for (BakedQuad quad : quads) {
            (quad.materialInfo().sprite().atlasLocation().equals(Sheets.BLOCKS_MAPPER.sheet())
                    ? blockAtlas : itemAtlas).add(quad);
        }
        submitAtlas(collector, poses, light, overlay, tints, blockAtlas, previewColor, true);
        submitAtlas(collector, poses, light, overlay, tints, itemAtlas, previewColor, false);
    }

    private static void submitAtlas(SubmitNodeCollector collector, PoseStack poses, int light, int overlay,
                                    int[] tints, List<BakedQuad> quads, int previewColor, boolean blockAtlas) {
        if (quads.isEmpty()) return;
        collector.submitCustomGeometry(poses,
                blockAtlas ? Sheets.translucentBlockItemSheet() : Sheets.translucentItemSheet(),
                (pose, buffer) -> {
                    QuadInstance instance = new QuadInstance();
                    instance.setLightCoords(light);
                    instance.setOverlayCoords(overlay);
                    for (BakedQuad quad : quads) {
                        BakedQuad.MaterialInfo material = quad.materialInfo();
                        int tint = material.isTinted() && material.tintIndex() < tints.length
                                ? tints[material.tintIndex()] : -1;
                        instance.setColor(ARGB.multiply(previewColor, tint));
                        buffer.putBakedQuad(pose, quad, instance);
                    }
                });
    }

    private static final class PreviewCollector implements InvocationHandler {
        private final SubmitNodeCollector proxy = (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(), new Class<?>[]{SubmitNodeCollector.class}, this);
        private SubmitNodeCollector delegate;
        private int previewColor;

        private void begin(SubmitNodeCollector delegate, int previewColor) {
            this.delegate = delegate;
            this.previewColor = previewColor;
        }

        private void end() {
            delegate = null;
        }

        private SubmitNodeCollector proxy() {
            return proxy;
        }

        @Override
        public Object invoke(Object ignored, Method method, Object[] arguments) throws Throwable {
            SubmitNodeCollector current = delegate;
            if (current == null) throw new IllegalStateException("Preview collector used outside a render submission");
            if (method.getName().equals("submitItem") && arguments != null && arguments.length == 8) {
                @SuppressWarnings("unchecked")
                List<BakedQuad> quads = (List<BakedQuad>) arguments[6];
                submitQuads(current, (PoseStack) arguments[0], (int) arguments[2],
                        (int) arguments[3], (int[]) arguments[5], quads, previewColor);
                return null;
            }
            try {
                return method.invoke(current, arguments);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }
    }

    private TranslucentItemPreviewRenderer() {
    }
}