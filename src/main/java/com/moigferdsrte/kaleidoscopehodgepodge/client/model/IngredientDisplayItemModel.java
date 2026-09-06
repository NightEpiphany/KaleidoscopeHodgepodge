package com.moigferdsrte.kaleidoscopehodgepodge.client.model;

import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class IngredientDisplayItemModel implements ItemModel {

    private static final Identifier DEFAULT = Identifier.fromNamespaceAndPath(KaleidoscopeHodgepodge.MOD_ID, "item/ingredient_display");
    private final Map<String, ItemModel> models;
    private final ItemModel fallback;

    private IngredientDisplayItemModel(Map<String, ItemModel> models, ItemModel fallback) {
        this.models = models;
        this.fallback = fallback;
    }

    @Override
    public void update(@NonNull ItemStackRenderState output, ItemStack item, @NonNull ItemModelResolver resolver,
                       @NonNull ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        String key = item.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL, "");
        models.getOrDefault(key, fallback).update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public @NonNull MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @NonNull ItemModel bake(ItemModel.@NonNull BakingContext context, @NonNull Matrix4fc transformation) {
            Map<String, ItemModel> models = new HashMap<>();
            for (PackingIngredients ingredient : PackingIngredients.values()) {
                if (
                        ingredient.getSrcFoodIds().stream().anyMatch(s -> s.getNamespace().equals("kaleidoscope_nether")) && !FabricLoader.getInstance().isModLoaded("kaleidoscope_nether")
                                || ingredient.getSrcFoodIds().stream().anyMatch(s -> s.getNamespace().equals("kaleidoscope_end")) && !FabricLoader.getInstance().isModLoaded("kaleidoscope_end")
                )
                    continue;
                models.put(ingredient.getResourceLoc(), model(ingredient).bake(context, transformation));
            }
            return new IngredientDisplayItemModel(Map.copyOf(models), fallbackModel().bake(context, transformation));
        }

        @Override
        public void resolveDependencies(ResolvableModel.@NonNull Resolver resolver) {
            for (PackingIngredients ingredient : PackingIngredients.values()) {
                model(ingredient).resolveDependencies(resolver);
            }
            fallbackModel().resolveDependencies(resolver);
        }

        private static CuboidItemModelWrapper.Unbaked fallbackModel() {
            return new CuboidItemModelWrapper.Unbaked(IngredientDisplayItemModel.DEFAULT, Optional.empty(), List.of());
        }

        private static CuboidItemModelWrapper.Unbaked model(PackingIngredients ingredient) {
            Identifier id = Identifier.fromNamespaceAndPath(KaleidoscopeHodgepodge.MOD_ID,
                    "item/" + ingredient.getResourceLoc());
            return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of());
        }
    }
}
