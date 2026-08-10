package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PackingIngredientRegistry {
    private static final Map<Identifier, PackingIngredients> BY_ID;
    private static final Map<Identifier, List<PackingIngredients>> BY_SOURCE;

    static {
        Map<Identifier, PackingIngredients> ids = new java.util.HashMap<>();
        Map<Identifier, java.util.ArrayList<PackingIngredients>> sources = new java.util.HashMap<>();
        for (PackingIngredients ingredient : PackingIngredients.values()) {
            ids.put(ingredient.getId(), ingredient);
            for (Identifier sourceId : ingredient.getSrcFoodIds()) {
                sources.computeIfAbsent(sourceId, ignored -> new java.util.ArrayList<>()).add(ingredient);
            }
        }
        BY_ID = Collections.unmodifiableMap(ids);
        Map<Identifier, List<PackingIngredients>> frozenSources = new java.util.HashMap<>();
        sources.forEach((id, values) -> frozenSources.put(id, List.copyOf(values)));
        BY_SOURCE = Collections.unmodifiableMap(frozenSources);
    }

    public static Optional<PackingIngredients> byId(Identifier id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<PackingIngredients> byId(String id) {
        try {
            return byId(Identifier.parse(id));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static List<PackingIngredients> bySource(Identifier sourceId) {
        return BY_SOURCE.getOrDefault(sourceId, List.of());
    }

    public static List<Identifier> sourceIdsFor(List<Identifier> ingredientIds) {
        return ingredientIds.stream()
                .map(BY_ID::get)
                .filter(java.util.Objects::nonNull)
                .flatMap(ingredient -> ingredient.getSrcFoodIds().stream())
                .distinct()
                .toList();
    }

    public static Map<Identifier, PackingIngredients> all() {
        return BY_ID;
    }

    private PackingIngredientRegistry() {}
}
