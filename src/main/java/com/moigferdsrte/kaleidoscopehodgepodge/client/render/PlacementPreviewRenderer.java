package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewPositionAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewRotationAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Vector3f;

/** Renders a translucent model at the currently valid placement target. */
public final class PlacementPreviewRenderer {
    private static final IngredientPreviewPositionAnimation POSITION = new IngredientPreviewPositionAnimation();
    private static final IngredientPreviewRotationAnimation ROTATION = new IngredientPreviewRotationAnimation();
    private static final Vector3f OFFSET = new Vector3f();
    private static float sampledRotation;
    private static long sampledAt;

    public static void beginFrame() {
        POSITION.beginFrame();
        ROTATION.beginFrame();
    }

    public static void render(RenderHighlightEvent.Block event, BlockPos pos,
                              PlacedIngredient placement, int placementIndex, int contentRevision) {
        prepare(pos, placement, placementIndex, contentRevision);
        renderPrepared(event, pos, placement);
    }

    /** Samples the preview transform once so its model and outline share an identical frame. */
    public static void prepare(BlockPos pos, PlacedIngredient placement,
                               int placementIndex, int contentRevision) {
        sampledAt = System.nanoTime();
        boolean animate = GeneralConfig.snapshot().placementAnimation();
        POSITION.update(pos, placement.id(), contentRevision, placement.x(), placement.y(), placement.z(),
                sampledAt, animate);
        ROTATION.update(pos, placement.id(), contentRevision, placement.rotation(), sampledAt, animate);
        POSITION.sample(sampledAt);
        sampledRotation = ROTATION.sample(sampledAt);
        if (GeneralConfig.snapshot().modelMicroOffset()) {
            IngredientRenderOffset.writeForPlacement(OFFSET, pos, placement, placementIndex);
        } else {
            OFFSET.set(0.0F, 0.0F, 0.0F);
        }
    }

    static void renderPrepared(RenderHighlightEvent.Block event, BlockPos pos,
                               PlacedIngredient placement) {
        float alpha = (float) GeneralConfig.snapshot().placementPreviewAlpha();
        if (alpha <= 0.0F) return;
        PackingIngredientRegistry.byId(placement.id()).ifPresent(ingredient -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;
            var camera = event.getCamera().getPosition();
            PoseStack poses = event.getPoseStack();
            poses.pushPose();
            poses.translate(pos.getX() - camera.x + POSITION.x() / 16.0 + OFFSET.x(),
                    pos.getY() - camera.y + POSITION.y() / 16.0 + 0.5 + OFFSET.y(),
                    pos.getZ() - camera.z + POSITION.z() / 16.0 + OFFSET.z());
            poses.mulPose(Axis.YP.rotationDegrees(sampledRotation));

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

    public static void drawAnimatedOutline(RenderHighlightEvent.Block event, BlockPos pos,
                                           PlacedIngredient placement, float[] color) {
        var camera = event.getCamera().getPosition();
        var lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(pos.getX() - camera.x + POSITION.x() / 16.0 + OFFSET.x(),
                pos.getY() - camera.y + POSITION.y() / 16.0 + 0.5 + OFFSET.y(),
                pos.getZ() - camera.z + POSITION.z() / 16.0 + OFFSET.z());
        poses.mulPose(Axis.YP.rotationDegrees(sampledRotation));
        double centerX = placement.x() / 16.0;
        double baseY = placement.y() / 16.0;
        double centerZ = placement.z() / 16.0;
        IngredientHitTest.localShapeBeforeRotation(placement).toAabbs().forEach(box ->
                LevelRenderer.renderLineBox(poses, lines,
                        box.move(-centerX, -baseY - 0.5, -centerZ),
                        color[0], color[1], color[2], color[3]));
        poses.popPose();
    }

    private PlacementPreviewRenderer() {
    }
}
