package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;

import java.util.List;
import java.util.Optional;

public final class PlacementSpace {
    public enum Failure { CAPACITY, OUT_OF_BOUNDS, OVERLAP }
    public record Result(Optional<PlacedIngredient> placement, Failure failure) {
        public static Result success(PlacedIngredient value) { return new Result(Optional.of(value), null); }
        public static Result failure(Failure value) { return new Result(Optional.empty(), value); }
        public boolean success() { return placement.isPresent(); }
    }

    public static Result place(List<PlacedIngredient> existing, PackingIngredients ingredient,
                               int hitX, int hitZ, CustomFeastData.ContainerKind kind) {
        int max = kind == CustomFeastData.ContainerKind.SOUP ? GeneralConfig.snapshot().soupCapacity()
                : GeneralConfig.snapshot().dishCapacity();
        if (existing.size() >= max) return Result.failure(Failure.CAPACITY);
        PackingIngredients.Size size = ingredient.getSize();
        int y = kind == CustomFeastData.ContainerKind.SOUP ? GeneralConfig.snapshot().soupBaseHeight()
                : GeneralConfig.snapshot().dishBaseHeight();
        for (PlacedIngredient item : existing) {
            if (containsColumn(item, hitX, hitZ)) {
                y = Math.max(y, item.y() + item.sizeY());
            }
        }
        PlacedIngredient candidate = new PlacedIngredient(ingredient.getId(), hitX, y, hitZ,
                size.x(), size.y(), size.z());
        if (!within(candidate)) return Result.failure(Failure.OUT_OF_BOUNDS);
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
        return item.xMin() >= 0 && item.xMax() <= 32
                && item.zMin() >= 0 && item.zMax() <= 32
                && item.y() >= 0 && item.y() + item.sizeY() <= GeneralConfig.snapshot().maxModelHeight();
    }

    private PlacementSpace() {}
}
