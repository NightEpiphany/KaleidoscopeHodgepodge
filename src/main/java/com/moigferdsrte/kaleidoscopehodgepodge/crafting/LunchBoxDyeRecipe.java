package com.moigferdsrte.kaleidoscopehodgepodge.crafting;

import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHRecipes;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class LunchBoxDyeRecipe extends CustomRecipe {
    public LunchBoxDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int boxes = 0;
        int dyes = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(KHItems.LUNCH_BOX.get())) boxes++;
            else if (stack.getItem() instanceof DyeItem) dyes++;
            else return false;
        }
        return boxes == 1 && dyes == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack box = ItemStack.EMPTY;
        DyeItem dye = null;
        for (ItemStack stack : input.items()) {
            if (stack.is(KHItems.LUNCH_BOX.get())) box = stack;
            else if (stack.getItem() instanceof DyeItem value) dye = value;
        }
        if (box.isEmpty() || dye == null) return ItemStack.EMPTY;
        ItemStack result = box.copyWithCount(1);
        result.set(DataComponents.DYED_COLOR, new DyedItemColor(LunchBoxItem.colorFor(dye.getDyeColor()), true));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return KHRecipes.LUNCH_BOX_DYE.get();
    }
}
