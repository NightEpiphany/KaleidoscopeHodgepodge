package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;

import java.util.List;
import java.util.Optional;

/*放置空间定义*/
public final class PlacementSpace {
    public enum Failure { CAPACITY, OUT_OF_BOUNDS, OVERLAP }
    public record Bounds(int minX, int maxX, int minZ, int maxZ, int maxHeight) {
        public static Bounds full(int maxHeight) {
            return new Bounds(0, 16, 0, 16, maxHeight);
        }
    }

    public record Result(Optional<PlacedIngredient> placement, Failure failure) {
        public static Result success(PlacedIngredient value) { return new Result(Optional.of(value), null); }
        public static Result failure(Failure value) { return new Result(Optional.empty(), value); }
        public boolean success() { return placement.isPresent(); }
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, CustomFeastData.ContainerKind kind) {
        GeneralConfig.Snapshot config = GeneralConfig.snapshot();
        int max = kind == CustomFeastData.ContainerKind.SOUP ? config.soupCapacity()
                : config.woodenPlateCapacity();
        int baseHeight = kind == CustomFeastData.ContainerKind.SOUP ? config.soupBaseHeight()
                : config.dishBaseHeight();
        int maxHeight = kind == CustomFeastData.ContainerKind.SOUP ? config.porcelainMaxModelHeight()
                : config.woodenMaxModelHeight();
        return place(existing, ingredient, hitX, hitZ, max, baseHeight, maxHeight);
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, int capacity, int baseHeight, int maxHeight) {
        return place(existing, ingredient, hitX, hitZ, capacity, baseHeight, maxHeight, 0);
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, int capacity, int baseHeight, int maxHeight,
                               int rotation) {
        return place(existing, ingredient, hitX, hitZ, capacity, baseHeight, maxHeight, rotation,
                IngredientFoodData.EMPTY);
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, int capacity, int baseHeight, int maxHeight,
                               int rotation, IngredientFoodData food) {
        return place(existing, ingredient, hitX, hitZ, capacity, baseHeight, Bounds.full(maxHeight),
                rotation, food);
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, int capacity, int baseHeight, Bounds bounds,
                               int rotation) {
        return place(existing, ingredient, hitX, hitZ, capacity, baseHeight, bounds, rotation,
                IngredientFoodData.EMPTY);
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, int capacity, int baseHeight, Bounds bounds,
                               int rotation, IngredientFoodData food) {
        int max = Math.max(0, capacity);
        if (existing.size() >= max) return Result.failure(Failure.CAPACITY);
        PackingIngredients.Size size = ingredient.getSize();
        int y = baseHeight;
        for (PlacedIngredient item : existing) {
            if (containsColumn(item, hitX, hitZ)) {
                y = Math.max(y, item.y() + item.sizeY());
            }
        }
        int normalizedRotation = Math.floorMod(rotation, 4);
        boolean swapsHorizontalAxes = normalizedRotation % 2 == 1;
        int sizeX = swapsHorizontalAxes ? size.z() : size.x();
        int sizeZ = swapsHorizontalAxes ? size.x() : size.z();
        PlacedIngredient candidate = new PlacedIngredient(ingredient.getId(), hitX, y, hitZ,
                sizeX, size.y(), sizeZ, normalizedRotation, food);
        if (!within(candidate, bounds)) return Result.failure(Failure.OUT_OF_BOUNDS);
        for (PlacedIngredient item : existing) {
            if (candidate.intersects(item)) return Result.failure(Failure.OVERLAP);
        }
        return Result.success(candidate);
    }

    private static boolean containsColumn(PlacedIngredient item, int x, int z) {
        return 2 * x >= item.xMin() && 2 * x < item.xMax()
                && 2 * z >= item.zMin() && 2 * z < item.zMax();
    }

    public static boolean within(PlacedIngredient item) {
        return within(item, GeneralConfig.snapshot().porcelainMaxModelHeight());
    }

    public static boolean within(PlacedIngredient item, int maxHeight) {
        return within(item, Bounds.full(maxHeight));
    }

    public static boolean within(PlacedIngredient item, Bounds bounds) {
        return item.xMin() >= 2 * bounds.minX() && item.xMax() <= 2 * bounds.maxX()
                && item.zMin() >= 2 * bounds.minZ() && item.zMax() <= 2 * bounds.maxZ()
                && item.y() >= 0 && item.y() + item.sizeY() <= bounds.maxHeight();
    }

    private PlacementSpace() {}
}
