package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.api.Service;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.fabricmc.api.EnvType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/*食材模型相关服务*/
@Service(usedFor = Service.UsedFor.BLOCK_ENTITY, env = EnvType.CLIENT)
public final class IngredientModelService {
    public static ItemStack createDisplay(ResourceLocation ingredientId) {
        ItemStack stack = KHItems.INGREDIENT_DISPLAY.getDefaultInstance();
        PackingIngredientRegistry.byId(ingredientId).ifPresent(ingredient ->
                stack.set(KHDataComponents.INGREDIENT_DISPLAY_MODEL, ingredient.getResourceLoc()));
        return stack;
    }

    public static ItemStack createDisplay(PackingIngredients ingredient) {
        return createDisplay(ingredient.getId());
    }

    public static ItemStack createDisplay(BaggedIngredient ingredient) {
        ItemStack stack = createDisplay(ingredient.id());
        stack.set(KHDataComponents.INGREDIENT_DISPLAY_ROTATION, ingredient.rotation());
        stack.set(KHDataComponents.INGREDIENT_DISPLAY_FOOD, ingredient.food());
        return stack;
    }

    public static @Nullable ResourceLocation resolveIngredientId(ItemStack stack) {
        String model = stack.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL, "");
        if (model.isBlank()) return null;
        return PackingIngredientRegistry.byModel(model)
                .map(PackingIngredients::getId)
                .orElse(null);
    }

    private IngredientModelService() {}
}
