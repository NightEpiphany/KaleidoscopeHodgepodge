package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TranslucentItemPreviewRendererTest {
    @Test
    void acceptsItemQuadsFromVanillaLayerSubmission() {
        ItemStackRenderState model = new ItemStackRenderState();
        model.newLayer().setQuads(ItemQuads.EMPTY);
        assertFalse(model.isEmpty());
        PoseStack poses = new PoseStack();

        assertDoesNotThrow(() -> TranslucentItemPreviewRenderer.submit(
                model, poses, rejectingCollector(), 0, 0, 0.5F, new Matrix4f()));

        assertTrue(poses.isEmpty());
    }

    @Test
    void handlesMultipleTintedAndFoilLayersAcrossSubmissions() {
        ItemStackRenderState model = new ItemStackRenderState();
        model.ensureCapacity(2);
        model.newLayer().setQuads(ItemQuads.EMPTY);
        ItemStackRenderState.LayerRenderState layer = model.newLayer();
        layer.setQuads(ItemQuads.EMPTY);
        layer.tintLayers().add(0xFF80FF40);
        layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);

        for (int frame = 0; frame < 2; frame++) {
            PoseStack poses = new PoseStack();
            assertDoesNotThrow(() -> TranslucentItemPreviewRenderer.submit(
                    model, poses, rejectingCollector(), 0, 0, 0.5F, new Matrix4f()));
            assertTrue(poses.isEmpty());
        }
    }

    private static SubmitNodeCollector rejectingCollector() {
        return (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, arguments) -> {
                    fail("Empty preview geometry must not reach the delegate: " + method.getName());
                    return null;
                });
    }
}
