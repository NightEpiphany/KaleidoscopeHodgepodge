package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.crafting.HodgepodgeRecipeResetRecipe;
import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class KHRecipes {
    public static final RecipeSerializer<com.moigferdsrte.kaleidoscopehodgepodge.crafting.LunchBoxDyeRecipe> LUNCH_BOX_DYE =
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, KaleidoscopeHodgepodge.id("lunch_box_dye"),
                    new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(
                            com.moigferdsrte.kaleidoscopehodgepodge.crafting.LunchBoxDyeRecipe::new));
    public static final RecipeSerializer<HodgepodgeRecipeResetRecipe> HODGEPODGE_RECIPE_RESET = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            KaleidoscopeHodgepodge.id("reset_hodgepodge_recipe"),
            new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(HodgepodgeRecipeResetRecipe::new)
    );

    public static void init() {
    }

    private KHRecipes() {
    }
}
