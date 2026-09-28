package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public final class IngredientGuiPreview {
    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        BaggedIngredient ingredient;
        if (stack.is(KHItems.WRAPPING_BAG) && GeneralConfig.snapshot().wrappingBagIngredientPreview()
                && !Screen.hasShiftDown()) {
            ingredient = PackingBagService.get(stack).first().orElse(null);
        } else if (stack.is(KHItems.LUNCH_BOX) && GeneralConfig.snapshot().lunchBoxIngredientPreview()) {
            ingredient = LunchBoxService.selectedIngredient(stack);
        } else return;
        if (ingredient == null) return;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + 8, y + 8, 200);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.renderItem(IngredientModelService.createDisplay(ingredient), 0, 0);
        } finally {
            graphics.pose().popPose();
        }
    }

    private IngredientGuiPreview() {}
}
