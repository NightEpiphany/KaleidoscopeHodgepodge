package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class IngredientModelService {
    public static ItemStack createDisplay(Identifier ingredientId) {
        ItemStack stack = KHItems.INGREDIENT_DISPLAY.getDefaultInstance();
        PackingIngredientRegistry.byId(ingredientId).ifPresent(ingredient ->
                stack.set(KHDataComponents.INGREDIENT_DISPLAY_MODEL, ingredient.getResourceLoc()));
        return stack;
    }

    public static ItemStack createDisplay(PackingIngredients ingredient) {
        return createDisplay(ingredient.getId());
    }

    private IngredientModelService() {}
}
