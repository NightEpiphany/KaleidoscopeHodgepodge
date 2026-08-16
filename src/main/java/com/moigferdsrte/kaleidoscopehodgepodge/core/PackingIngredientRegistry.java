package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.neoforged.fml.ModList;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PackingIngredientRegistry {
    private static final Map<ResourceLocation, PackingIngredients> BY_ID;
    private static final Map<ResourceLocation, List<PackingIngredients>> BY_SOURCE;

    static {
        Map<ResourceLocation, PackingIngredients> ids = new java.util.HashMap<>();
        Map<ResourceLocation, java.util.ArrayList<PackingIngredients>> sources = new java.util.HashMap<>();
        for (PackingIngredients ingredient : PackingIngredients.values()) {
            if (
                    ingredient.getSrcFoodIds().stream().anyMatch(s -> s.getNamespace().equals("kaleidoscope_nether")) && !ModList.get().isLoaded("kaleidoscope_nether")
                            || ingredient.getSrcFoodIds().stream().anyMatch(s -> s.getNamespace().equals("kaleidoscope_end")) && !ModList.get().isLoaded("kaleidoscope_end")
            )
                continue;
            ids.put(ingredient.getId(), ingredient);
            for (ResourceLocation sourceId : ingredient.getSrcFoodIds()) {
                sources.computeIfAbsent(sourceId, ignored -> new java.util.ArrayList<>()).add(ingredient);
            }
        }
        BY_ID = Collections.unmodifiableMap(ids);
        Map<ResourceLocation, List<PackingIngredients>> frozenSources = new java.util.HashMap<>();
        sources.forEach((id, values) -> frozenSources.put(id, List.copyOf(values)));
        BY_SOURCE = Collections.unmodifiableMap(frozenSources);
    }

    public static Optional<PackingIngredients> byId(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<PackingIngredients> byId(String id) {
        try {
            return byId(ResourceLocation.parse(id));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static List<PackingIngredients> bySource(ResourceLocation sourceId) {
        return BY_SOURCE.getOrDefault(sourceId, List.of());
    }

    public static List<ResourceLocation> sourceIdsFor(List<ResourceLocation> ingredientIds) {
        return ingredientIds.stream()
                .map(BY_ID::get)
                .filter(java.util.Objects::nonNull)
                .flatMap(ingredient -> ingredient.getSrcFoodIds().stream())
                .distinct()
                .toList();
    }

    public static Map<ResourceLocation, PackingIngredients> all() {
        return BY_ID;
    }

    private PackingIngredientRegistry() {}
}
