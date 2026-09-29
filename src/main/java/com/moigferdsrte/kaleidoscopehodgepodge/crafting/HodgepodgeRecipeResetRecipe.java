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
import org.jetbrains.annotations.NotNull;

/** 重置配方 */
public final class HodgepodgeRecipeResetRecipe extends CustomRecipe {
    public HodgepodgeRecipeResetRecipe(net.minecraft.world.item.crafting.CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        if (input.ingredientCount() != 1) return false;

        ItemStack stack = input.items().stream().filter(candidate -> !candidate.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        if (!stack.is(KHItems.HODGEPODGE_RECIPE.get())) return false;

        HodgepodgeRecipeData data = stack.get(KHDataComponents.HODGEPODGE_RECIPE.get());
        if (data == null) return false;
        try {
            FeastCodec.decode(data.feastCode());
            return true;
        } catch (FeastCodec.FormatException exception) {
            return false;
        }
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return new ItemStack(ModItems.RECIPE_ITEM.get());
    }

    @Override
    public @NotNull RecipeSerializer<HodgepodgeRecipeResetRecipe> getSerializer() {
        return KHRecipes.HODGEPODGE_RECIPE_RESET.get();
    }
}
