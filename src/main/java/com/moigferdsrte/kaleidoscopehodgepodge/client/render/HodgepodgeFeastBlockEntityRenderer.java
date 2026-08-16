package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientBounceAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public final class HodgepodgeFeastBlockEntityRenderer
        implements BlockEntityRenderer<HodgepodgeFeastBlockEntity> {
    private final ItemRenderer itemRenderer;

    public HodgepodgeFeastBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(HodgepodgeFeastBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource consumers, int light, int overlay) {
        var ingredients = entity.renderIngredients();
        for (int index = 0; index < ingredients.size(); index++) {
            var ingredient = ingredients.get(index);
            IngredientBounceAnimation.Scale scale = GeneralConfig.snapshot().placementAnimation()
                    && index == entity.placementAnimationIndex()
                    ? IngredientBounceAnimation.sample(
                    (System.nanoTime() - entity.placementAnimationStartedAt()) / 1_000_000L)
                    : IngredientBounceAnimation.Scale.IDENTITY;
            var offset = GeneralConfig.snapshot().modelMicroOffset()
                    ? IngredientRenderOffset.forPlacement(entity.getBlockPos(), ingredient, index)
                    : new org.joml.Vector3f();
            poses.pushPose();
            poses.translate(ingredient.x() / 16.0 + offset.x(),
                    ingredient.y() / 16.0 + 0.5 * scale.vertical() + offset.y(),
                    ingredient.z() / 16.0 + offset.z());
            poses.mulPose(Axis.YP.rotationDegrees(-90.0F * ingredient.rotation()));
            poses.scale(scale.horizontal(), scale.vertical(), scale.horizontal());
            var stack = IngredientModelService.createDisplay(ingredient.id());
            ResourceLocation modelId = com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry
                    .byId(ingredient.id())
                    .map(value -> KaleidoscopeHodgepodge.id("item/" + value.getResourceLoc()))
                    .orElseGet(() -> KaleidoscopeHodgepodge.id("item/wrapping_bag"));
            var model = ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(modelId);
            itemRenderer.render(stack, ItemDisplayContext.NONE, false, poses, consumers, light, overlay, model);
            poses.popPose();
        }
    }
}
