package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.math.Axis;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewPositionAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewRotationAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class PlacementPreviewRenderer {
    private static final IngredientPreviewPositionAnimation POSITION = new IngredientPreviewPositionAnimation();
    private static final IngredientPreviewRotationAnimation ROTATION = new IngredientPreviewRotationAnimation();
    private static final Vector3f OFFSET = new Vector3f();
    private static float sampledRotation;

    public static void beginFrame() {
        POSITION.beginFrame();
        ROTATION.beginFrame();
    }

    public static void render(WorldRenderContext context, BlockPos pos,
                              PlacedIngredient placement, int placementIndex, int contentRevision) {
        prepare(pos, placement, placementIndex, contentRevision);
        renderPrepared(context, pos, placement);
    }

    /** Samples the preview transform once so its model and outline share an identical frame. */
    public static void prepare(BlockPos pos, PlacedIngredient placement,
                               int placementIndex, int contentRevision) {
        long now = System.nanoTime();
        boolean animate = GeneralConfig.snapshot().placementAnimation();
        POSITION.update(pos, placement.id(), contentRevision, placement.x(), placement.y(), placement.z(), now, animate);
        ROTATION.update(pos, placement.id(), contentRevision, placement.rotation(), now, animate);
        POSITION.sample(now);
        sampledRotation = ROTATION.sample(now);
        if (GeneralConfig.snapshot().modelMicroOffset()) {
            IngredientRenderOffset.writeForPlacement(OFFSET, pos, placement, placementIndex);
        } else {
            OFFSET.set(0.0F, 0.0F, 0.0F);
        }
    }

    static void renderPrepared(WorldRenderContext context, BlockPos pos, PlacedIngredient placement) {
        float alpha = (float) GeneralConfig.snapshot().placementPreviewAlpha();
        if (alpha <= 0.0F) return;

        PackingIngredientRegistry.byId(placement.id()).ifPresent(ingredient -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;
            var camera = context.camera().getPosition();
            var poses = context.matrixStack();
            poses.pushPose();
            poses.translate(pos.getX() - camera.x + POSITION.x() / 16.0 + OFFSET.x(),
                    pos.getY() - camera.y + POSITION.y() / 16.0 + 0.5 + OFFSET.y(),
                    pos.getZ() - camera.z + POSITION.z() / 16.0 + OFFSET.z());
            poses.mulPose(Axis.YP.rotationDegrees(sampledRotation));

            var stack = IngredientModelService.createDisplay(placement.id());
            var modelId = KaleidoscopeHodgepodge.id("item/" + ingredient.getResourceLoc());
            var model = ((FabricBakedModelManager) minecraft.getModelManager()).getModel(modelId);
            minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poses,
                    new AlphaMultiBufferSource(context.consumers(), alpha),
                    LevelRenderer.getLightColor(minecraft.level, pos), OverlayTexture.NO_OVERLAY, model);
            poses.popPose();
        });
    }

    public static void drawAnimatedOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outline,
                                           PlacedIngredient placement, float[] color) {
        BlockPos pos = outline.blockPos();
        var poses = context.matrixStack();
        poses.pushPose();
        poses.translate(pos.getX() - outline.cameraX() + POSITION.x() / 16.0 + OFFSET.x(),
                pos.getY() - outline.cameraY() + POSITION.y() / 16.0 + 0.5 + OFFSET.y(),
                pos.getZ() - outline.cameraZ() + POSITION.z() / 16.0 + OFFSET.z());
        poses.mulPose(Axis.YP.rotationDegrees(sampledRotation));
        double centerX = placement.x() / 16.0;
        double baseY = placement.y() / 16.0;
        double centerZ = placement.z() / 16.0;
        IngredientHitTest.localShapeBeforeRotation(placement).toAabbs().forEach(box ->
                LevelRenderer.renderLineBox(poses, outline.vertexConsumer(),
                        box.move(-centerX, -baseY - 0.5, -centerZ),
                        color[0], color[1], color[2], color[3]));
        poses.popPose();
    }

    private PlacementPreviewRenderer() {
    }
}
