package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class IngredientGuiPreview {
    public static void render(
            ItemRenderer renderer,
            ItemStack stack,
            ItemDisplayContext renderMode,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay) {
        if (renderMode != ItemDisplayContext.GUI) return;

        BaggedIngredient ingredient;
        if (stack.is(KHItems.WRAPPING_BAG.get()) && GeneralConfig.snapshot().wrappingBagIngredientPreview()
                && !Screen.hasShiftDown()) {
            ingredient = PackingBagService.get(stack).first().orElse(null);
        } else if (stack.is(KHItems.LUNCH_BOX.get()) && GeneralConfig.snapshot().lunchBoxIngredientPreview()) {
            ingredient = LunchBoxService.selectedIngredient(stack);
        } else return;
        if (ingredient == null) return;

        ItemStack display = IngredientModelService.createDisplay(ingredient);
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = renderer.getModel(display, minecraft.level, minecraft.player, 0);

        poseStack.pushPose();
        try {
            poseStack.translate(0.25F, -0.25F, 6.0F);
            poseStack.scale(0.685F, 0.685F, 0.685F);
            renderer.render(display, ItemDisplayContext.GUI, false, poseStack, bufferSource,
                    light, overlay, model);
        } finally {
            poseStack.popPose();
        }
    }

    private IngredientGuiPreview() {}
}
