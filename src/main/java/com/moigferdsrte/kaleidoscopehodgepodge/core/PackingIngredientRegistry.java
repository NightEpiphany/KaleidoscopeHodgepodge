package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.compat.Compat;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.neoforged.fml.ModList;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 食材模型实际注册类
 * @see PackingIngredients
 */
public final class PackingIngredientRegistry {
    private static final Map<ResourceLocation, PackingIngredients> BY_ID;
    private static final Map<String, PackingIngredients> BY_MODEL;
    private static final Map<ResourceLocation, List<PackingIngredients>> BY_SOURCE;

    static {
        int capacity = PackingIngredients.values().length * 2;
        Map<ResourceLocation, PackingIngredients> ids = new java.util.HashMap<>(capacity);
        Map<String, PackingIngredients> models = new java.util.HashMap<>(capacity);
        Map<ResourceLocation, java.util.ArrayList<PackingIngredients>> sources = new java.util.HashMap<>(capacity);
        for (PackingIngredients ingredient : PackingIngredients.values()) {
            if (ingredient.getSrcFoodIds().stream().noneMatch(PackingIngredientRegistry::sourceModLoaded)) continue;
            ids.put(ingredient.getId(), ingredient);
            models.putIfAbsent(ingredient.getResourceLoc(), ingredient);
            for (ResourceLocation sourceId : ingredient.getSrcFoodIds()) {
                if (!sourceModLoaded(sourceId)) continue;
                sources.computeIfAbsent(sourceId, ignored -> new java.util.ArrayList<>(capacity)).add(ingredient);
            }
        }
        BY_ID = Collections.unmodifiableMap(ids);
        BY_MODEL = Collections.unmodifiableMap(models);
        Map<ResourceLocation, List<PackingIngredients>> frozenSources = new java.util.HashMap<>(capacity);
        sources.forEach((id, values) -> frozenSources.put(id, List.copyOf(values)));
        BY_SOURCE = Collections.unmodifiableMap(frozenSources);
    }

    private static boolean sourceModLoaded(ResourceLocation id) {
        String namespace = id.getNamespace();
        return !namespace.equals(Compat.KN) && !namespace.equals(Compat.KE) && !namespace.equals(Compat.KCH)
                || ModList.get().isLoaded(namespace);
    }

    public static Optional<PackingIngredients> byId(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<PackingIngredients> byModel(String model) {
        return Optional.ofNullable(BY_MODEL.get(model));
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
