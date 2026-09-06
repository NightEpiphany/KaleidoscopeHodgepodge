package com.moigferdsrte.kaleidoscopehodgepodge.crafting;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FeastCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.core.HodgepodgeRecipeData;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

/** 重置配方 */
public final class HodgepodgeRecipeResetRecipe extends CustomRecipe {
    @Override
    public boolean matches(CraftingInput input, @NonNull Level level) {
        if (input.ingredientCount() != 1) return false;

        ItemStack stack = input.items().stream().filter(candidate -> !candidate.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        if (!stack.is(KHItems.HODGEPODGE_RECIPE)) return false;

        HodgepodgeRecipeData data = stack.get(KHDataComponents.HODGEPODGE_RECIPE);
        if (data == null) return false;
        try {
            FeastCodec.decode(data.feastCode());
            return true;
        } catch (FeastCodec.FormatException exception) {
            return false;
        }
    }

    @Override
    public @NonNull ItemStack assemble(@NonNull CraftingInput input) {
        return new ItemStack(ModItems.RECIPE_ITEM);
    }

    @Override
    public @NonNull RecipeSerializer<HodgepodgeRecipeResetRecipe> getSerializer() {
        return KHRecipes.HODGEPODGE_RECIPE_RESET;
    }
}
