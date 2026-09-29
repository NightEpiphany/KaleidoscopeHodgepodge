package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Vector3f;

/** Renders a translucent model at the currently valid placement target. */
public final class PlacementPreviewRenderer {
    private static final com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewPositionAnimation POSITION =
            new com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewPositionAnimation();
    private static final com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewRotationAnimation ROTATION =
            new com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewRotationAnimation();

    public static void beginFrame() {
        POSITION.beginFrame();
        ROTATION.beginFrame();
    }

    public static void render(RenderHighlightEvent.Block event, BlockPos pos,
                              PlacedIngredient placement, int placementIndex, int contentRevision) {
        float alpha = (float) GeneralConfig.snapshot().placementPreviewAlpha();
        if (alpha <= 0.0F) return;

        PackingIngredientRegistry.byId(placement.id()).ifPresent(ingredient -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;
            long now = System.nanoTime();
            boolean animate = GeneralConfig.snapshot().placementAnimation();
            POSITION.update(pos, placement.id(), contentRevision, placement.x(), placement.y(), placement.z(), now, animate);
            ROTATION.update(pos, placement.id(), contentRevision, placement.rotation(), now, animate);
            POSITION.sample(now);

            Vector3f offset = GeneralConfig.snapshot().modelMicroOffset()
                    ? IngredientRenderOffset.forPlacement(pos, placement, placementIndex)
                    : new Vector3f();
            var camera = event.getCamera().getPosition();
            PoseStack poses = event.getPoseStack();
            poses.pushPose();
            poses.translate(pos.getX() - camera.x + POSITION.x() / 16.0 + offset.x(),
                    pos.getY() - camera.y + POSITION.y() / 16.0 + 0.5 + offset.y(),
                    pos.getZ() - camera.z + POSITION.z() / 16.0 + offset.z());
            poses.mulPose(Axis.YP.rotationDegrees(ROTATION.sample(now)));

            var stack = IngredientModelService.createDisplay(placement.id());
            var modelId = ModelResourceLocation.standalone(
                    KaleidoscopeHodgepodge.id("item/" + ingredient.getResourceLoc()));
            var model = minecraft.getModelManager().getModel(modelId);
            minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poses,
                    new AlphaMultiBufferSource(event.getMultiBufferSource(), alpha),
                    LevelRenderer.getLightColor(minecraft.level, pos), OverlayTexture.NO_OVERLAY, model);
            poses.popPose();
        });
    }

    private PlacementPreviewRenderer() {
    }
}
