package com.moigferdsrte.kaleidoscopehodgepodge.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.OptionalInt;

public final class IngredientHitTest {
    public static OptionalInt nearest(List<PlacedIngredient> ingredients, BlockPos blockPos, Vec3 from, Vec3 to) {
        int nearestIndex = -1;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < ingredients.size(); i++) {
            var intersection = bounds(ingredients.get(i), blockPos).clip(from, to);
            if (intersection.isEmpty()) continue;
            double distance = from.distanceToSqr(intersection.orElseThrow());
            if (distance < nearestDistance) {
                nearestIndex = i;
                nearestDistance = distance;
            }
        }
        return nearestIndex < 0 ? OptionalInt.empty() : OptionalInt.of(nearestIndex);
    }

    public static AABB bounds(PlacedIngredient ingredient, BlockPos blockPos) {
        return new AABB(
                blockPos.getX() + ingredient.xMin() / 32.0,
                blockPos.getY() + ingredient.y() / 16.0,
                blockPos.getZ() + ingredient.zMin() / 32.0,
                blockPos.getX() + ingredient.xMax() / 32.0,
                blockPos.getY() + (ingredient.y() + ingredient.sizeY()) / 16.0,
                blockPos.getZ() + ingredient.zMax() / 32.0
        );
    }

    private IngredientHitTest() {}
}
