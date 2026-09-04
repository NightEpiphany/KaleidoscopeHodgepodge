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

    /** 单方块容器的便捷入口：摆放空间恰好等于方块自身的 0..16 像素范围。 */
    public static Optional<Pixel> resolve(List<PlacedIngredient> existing, BlockPos blockPos, Vec3 eye,
                                          BlockHitResult hit, PackingIngredients ingredient, int rotation) {
        return resolve(existing, blockPos, eye, hit, ingredient, rotation,
                PlacementSpace.Bounds.full(0), false);
    }

    /**
     * 解析放置像素。
     *
     * <p>{@code bounds} 必须是目标方块自身坐标系下的摆放空间；多方块容器（中/大瓷盘）的
     * 摆放空间会超出 {@code [0,16)}，若按单方块范围钳制，会把跨接缝的命中点吸附到方块
     * 接缝上，从而出现"对着模型顶面却放到旁边"的现象。
     */
    public static Optional<Pixel> resolve(List<PlacedIngredient> existing, BlockPos blockPos, Vec3 eye,
                                          BlockHitResult hit, PackingIngredients ingredient, int rotation,
                                          PlacementSpace.Bounds bounds, boolean allowBoundaryProjection) {
        Vec3 ray = hit.getLocation().subtract(eye);
        Vec3 end = ray.lengthSqr() > 1.0E-7
                ? hit.getLocation().add(ray.normalize().scale(1.0 / 16.0))
                : hit.getLocation();
        IngredientHitTest.Hit ingredientHit = IngredientHitTest.nearestHit(existing, blockPos, eye, end).orElse(null);

        if (hit.getDirection() == Direction.UP) {
            // 方块 outline 是容器与全部材料的并集，命中点本身无法指明命中了哪个材料的顶面，
            // 因此借助材料 AABB 求交结果，把投影像素吸附进该材料自身的列区间，保证
            // "只要对准顶面就一定叠放在其上"，不受视距与俯角影响。
            // 不能用 IngredientHitTest#face() 判定：命中点正好落在顶面边棱时，面判定会因
            // 距离并列而优先返回侧面。这里直接检查入射点是否位于该模型的顶平面。
            if (ingredientHit != null) {
                PlacedIngredient below = existing.get(ingredientHit.index());
                if (onTopPlane(below, blockPos, ingredientHit.location())) {
                    return Optional.of(stackedPixel(below, blockPos, ingredientHit.location(), bounds));
                }
            }
            return projectedPixel(blockPos, hit, bounds);
        }
        if (hit.getDirection().getAxis() == Direction.Axis.Y) return Optional.empty();

        if (ingredientHit == null) {
            return allowBoundaryProjection ? projectedPixel(blockPos, hit, bounds) : Optional.empty();
        }
        if (ingredientHit.face().getAxis() == Direction.Axis.Y) return Optional.empty();

        PlacedIngredient target = existing.get(ingredientHit.index());
        PackingIngredients.Size size = ingredient.getSize();
        boolean swapAxes = Math.floorMod(rotation, 4) % 2 == 1;
        int sizeX = swapAxes ? size.z() : size.x();
        int sizeZ = swapAxes ? size.x() : size.z();
        int x = pixel(ingredientHit.location().x - blockPos.getX(), bounds.minX(), bounds.maxX());
        int z = pixel(ingredientHit.location().z - blockPos.getZ(), bounds.minZ(), bounds.maxZ());
        return switch (ingredientHit.face()) {
            case WEST -> Optional.of(new Pixel(Math.floorDiv(target.xMin() - sizeX, 2), z));
            case EAST -> Optional.of(new Pixel(Math.floorDiv(target.xMax() + sizeX + 1, 2), z));
            case NORTH -> Optional.of(new Pixel(x, Math.floorDiv(target.zMin() - sizeZ, 2)));
            case SOUTH -> Optional.of(new Pixel(x, Math.floorDiv(target.zMax() + sizeZ + 1, 2)));
            default -> Optional.empty();
        };
    }

    /** 入射点是否落在该模型顶平面上（射线求交的浮点误差量级与 AABB#clip 相同）。 */
    private static boolean onTopPlane(PlacedIngredient target, BlockPos blockPos, Vec3 location) {
        double top = blockPos.getY() + (target.y() + target.sizeY()) / 16.0;
        return Math.abs(location.y - top) < 1.0E-6;
    }

    /** 命中材料顶面时的像素：先投影，再夹入该材料的合法堆叠列区间。 */
    private static Pixel stackedPixel(PlacedIngredient target, BlockPos blockPos, Vec3 location,
                                      PlacementSpace.Bounds bounds) {
        int x = clamp(pixel(location.x - blockPos.getX(), bounds.minX(), bounds.maxX()),
                columnMin(target.xMin()), columnMax(target.xMax()));
        int z = clamp(pixel(location.z - blockPos.getZ(), bounds.minZ(), bounds.maxZ()),
                columnMin(target.zMin()), columnMax(target.zMax()));
        return new Pixel(x, z);
    }

    private static Optional<Pixel> projectedPixel(BlockPos blockPos, BlockHitResult hit,
                                                  PlacementSpace.Bounds bounds) {
        return projectedPixel(blockPos, hit.getLocation(), bounds);
    }

    private static Optional<Pixel> projectedPixel(BlockPos blockPos, Vec3 location,
                                                  PlacementSpace.Bounds bounds) {
        return Optional.of(new Pixel(pixel(location.x - blockPos.getX(), bounds.minX(), bounds.maxX()),
                pixel(location.z - blockPos.getZ(), bounds.minZ(), bounds.maxZ())));
    }

    /**
     * 把方块局部坐标（单位：方块）投影为模型中心像素。模型中心落在像素网格线上
     * （{@code xMin = 2x - sizeX} 的半像素坐标系），因此取最近网格线而非向下取整，
     * 避免恒定半像素偏移导致顶面边缘命中掉出合法列区间。
     */
    private static int pixel(double localCoordinate, int minPixel, int maxPixel) {
        int projected = (int) Math.floor(localCoordinate * 16.0 + 0.5);
        return clamp(projected, minPixel, maxPixel - 1);
    }

    /** {@code PlacementSpace#containsColumn} 要求 {@code 2p ∈ [min, max)}，故列下界为 ceil(min/2)。 */
    private static int columnMin(int halfPixelMin) {
        return -Math.floorDiv(-halfPixelMin, 2);
    }

    /** 列上界为 ceil(max/2) - 1。 */
    private static int columnMax(int halfPixelMax) {
        return -Math.floorDiv(-halfPixelMax, 2) - 1;
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private IngredientPlacementTarget() {}
}
