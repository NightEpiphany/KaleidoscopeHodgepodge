package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

final class AlphaMultiBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final float alpha;

    AlphaMultiBufferSource(MultiBufferSource delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new AlphaVertexConsumer(delegate.getBuffer(previewRenderType(renderType)), alpha);
    }

    private static RenderType previewRenderType(RenderType renderType) {
        if (renderType == NeoForgeRenderTypes.ITEM_LAYERED_SOLID.get()
                || renderType == NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT.get()
                || renderType == NeoForgeRenderTypes.ITEM_LAYERED_CUTOUT_MIPPED.get()) {
            return NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get();
        }
        if (renderType == Sheets.solidBlockSheet() || renderType == Sheets.cutoutBlockSheet()) {
            return Sheets.translucentCullBlockSheet();
        }
        return renderType;
    }

    private record AlphaVertexConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int sourceAlpha) {
            delegate.setColor(red, green, blue, Mth.clamp(Math.round(sourceAlpha * alpha), 0, 255));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
