package com.moigferdsrte.kaleidoscopehodgepodge.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class IngredientHitTest {
    public record Hit(int index, Vec3 location, Direction face) {}

    public static OptionalInt nearest(List<PlacedIngredient> ingredients, BlockPos blockPos, Vec3 from, Vec3 to) {
        return nearestHit(ingredients, blockPos, from, to)
                .map(hit -> OptionalInt.of(hit.index()))
                .orElseGet(OptionalInt::empty);
    }

    public static Optional<Hit> nearestHit(List<PlacedIngredient> ingredients, BlockPos blockPos,
                                           Vec3 from, Vec3 to) {
        int nearestIndex = -1;
        double nearestDistance = Double.POSITIVE_INFINITY;
        Vec3 nearestLocation = null;
        AABB nearestBounds = null;
        for (int i = 0; i < ingredients.size(); i++) {
            AABB bounds = bounds(ingredients.get(i), blockPos);
            var intersection = bounds.clip(from, to);
            if (intersection.isEmpty()) continue;
            Vec3 location = intersection.orElseThrow();
            double distance = from.distanceToSqr(location);
            if (distance < nearestDistance) {
                nearestIndex = i;
                nearestDistance = distance;
                nearestLocation = location;
                nearestBounds = bounds;
            }
        }
        if (nearestIndex < 0 || nearestLocation == null || nearestBounds == null) return Optional.empty();
        return Optional.of(new Hit(nearestIndex, nearestLocation, face(nearestBounds, nearestLocation)));
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

    public static VoxelShape localShape(PlacedIngredient ingredient) {
        return Shapes.box(
                ingredient.xMin() / 32.0,
                ingredient.y() / 16.0,
                ingredient.zMin() / 32.0,
                ingredient.xMax() / 32.0,
                (ingredient.y() + ingredient.sizeY()) / 16.0,
                ingredient.zMax() / 32.0
        );
    }

    private static Direction face(AABB bounds, Vec3 location) {
        double nearest = Math.abs(location.x - bounds.minX);
        Direction face = Direction.WEST;
        if (Math.abs(location.x - bounds.maxX) < nearest) {
            nearest = Math.abs(location.x - bounds.maxX);
            face = Direction.EAST;
        }
        if (Math.abs(location.y - bounds.minY) < nearest) {
            nearest = Math.abs(location.y - bounds.minY);
            face = Direction.DOWN;
        }
        if (Math.abs(location.y - bounds.maxY) < nearest) {
            nearest = Math.abs(location.y - bounds.maxY);
            face = Direction.UP;
        }
        if (Math.abs(location.z - bounds.minZ) < nearest) {
            nearest = Math.abs(location.z - bounds.minZ);
            face = Direction.NORTH;
        }
        if (Math.abs(location.z - bounds.maxZ) < nearest) face = Direction.SOUTH;
        return face;
    }

    private IngredientHitTest() {}
}
