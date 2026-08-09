package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** 将容器或已有材料表面的射线命中转换为模型中心像素坐标。 */
public final class IngredientPlacementTarget {
    public record Pixel(int x, int z) {}

    public static Optional<Pixel> resolve(List<PlacedIngredient> existing, BlockPos blockPos, Vec3 eye,
                                          BlockHitResult hit, PackingIngredients ingredient, int rotation) {
        if (hit.getDirection() == Direction.UP) {
            return Optional.of(new Pixel(pixel(hit.getLocation().x - blockPos.getX()),
                    pixel(hit.getLocation().z - blockPos.getZ())));
        }
        if (hit.getDirection().getAxis() == Direction.Axis.Y) return Optional.empty();

        Vec3 ray = hit.getLocation().subtract(eye);
        Vec3 end = ray.lengthSqr() > 1.0E-7
                ? hit.getLocation().add(ray.normalize().scale(1.0 / 16.0))
                : hit.getLocation();
        IngredientHitTest.Hit ingredientHit = IngredientHitTest.nearestHit(existing, blockPos, eye, end).orElse(null);
        if (ingredientHit == null || ingredientHit.face().getAxis() == Direction.Axis.Y) return Optional.empty();

        PlacedIngredient target = existing.get(ingredientHit.index());
        PackingIngredients.Size size = ingredient.getSize();
        boolean swapAxes = Math.floorMod(rotation, 4) % 2 == 1;
        int sizeX = swapAxes ? size.z() : size.x();
        int sizeZ = swapAxes ? size.x() : size.z();
        int x = pixel(ingredientHit.location().x - blockPos.getX());
        int z = pixel(ingredientHit.location().z - blockPos.getZ());
        return switch (ingredientHit.face()) {
            case WEST -> Optional.of(new Pixel(Math.floorDiv(target.xMin() - sizeX, 2), z));
            case EAST -> Optional.of(new Pixel(Math.floorDiv(target.xMax() + sizeX + 1, 2), z));
            case NORTH -> Optional.of(new Pixel(x, Math.floorDiv(target.zMin() - sizeZ, 2)));
            case SOUTH -> Optional.of(new Pixel(x, Math.floorDiv(target.zMax() + sizeZ + 1, 2)));
            default -> Optional.empty();
        };
    }

    private static int pixel(double localCoordinate) {
        return Math.max(0, Math.min(15, (int) Math.floor(localCoordinate * 16.0)));
    }

    private IngredientPlacementTarget() {}
}
