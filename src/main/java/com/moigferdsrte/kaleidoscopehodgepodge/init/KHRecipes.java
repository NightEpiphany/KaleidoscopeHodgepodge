package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.crafting.HodgepodgeRecipeResetRecipe;
import com.moigferdsrte.kaleidoscopehodgepodge.crafting.LunchBoxDyeRecipe;
import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class KHRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, KaleidoscopeHodgepodge.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LunchBoxDyeRecipe>> LUNCH_BOX_DYE =
            SERIALIZERS.register("lunch_box_dye", () ->
                    new SimpleCraftingRecipeSerializer<>(
                            LunchBoxDyeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HodgepodgeRecipeResetRecipe>> HODGEPODGE_RECIPE_RESET = SERIALIZERS.register(
            "reset_hodgepodge_recipe",
            () -> new SimpleCraftingRecipeSerializer<>(HodgepodgeRecipeResetRecipe::new)
    );

    public static void init(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    private KHRecipes() {
    }
}
