package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.crafting.HodgepodgeRecipeResetRecipe;
import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class KHRecipes {
    public static final RecipeSerializer<HodgepodgeRecipeResetRecipe> HODGEPODGE_RECIPE_RESET = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            KaleidoscopeHodgepodge.id("reset_hodgepodge_recipe"),
            new RecipeSerializer<>(
                    MapCodec.unit(HodgepodgeRecipeResetRecipe::new),
                    StreamCodec.unit(new HodgepodgeRecipeResetRecipe())
            )
    );

    public static void init() {
    }

    private KHRecipes() {
    }
}
