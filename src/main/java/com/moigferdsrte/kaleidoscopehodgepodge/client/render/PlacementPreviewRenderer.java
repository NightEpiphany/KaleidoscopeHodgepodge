package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.math.Axis;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
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
    public static void render(WorldRenderContext context, BlockPos pos,
                              PlacedIngredient placement, int placementIndex) {
        float alpha = (float) GeneralConfig.snapshot().placementPreviewAlpha();
        if (alpha <= 0.0F) return;

        PackingIngredientRegistry.byId(placement.id()).ifPresent(ingredient -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;

            Vector3f offset = GeneralConfig.snapshot().modelMicroOffset()
                    ? IngredientRenderOffset.forPlacement(pos, placement, placementIndex)
                    : new Vector3f();
            var camera = context.camera().getPosition();
            var poses = context.matrixStack();
            poses.pushPose();
            poses.translate(pos.getX() - camera.x + placement.x() / 16.0 + offset.x(),
                    pos.getY() - camera.y + placement.y() / 16.0 + 0.5 + offset.y(),
                    pos.getZ() - camera.z + placement.z() / 16.0 + offset.z());
            poses.mulPose(Axis.YP.rotationDegrees(-90.0F * placement.rotation()));

            var stack = IngredientModelService.createDisplay(placement.id());
            var modelId = KaleidoscopeHodgepodge.id("item/" + ingredient.getResourceLoc());
            var model = ((FabricBakedModelManager) minecraft.getModelManager()).getModel(modelId);
            minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poses,
                    new AlphaMultiBufferSource(context.consumers(), alpha),
                    LevelRenderer.getLightColor(minecraft.level, pos), OverlayTexture.NO_OVERLAY, model);
            poses.popPose();
        });
    }

    private PlacementPreviewRenderer() {
    }
}
