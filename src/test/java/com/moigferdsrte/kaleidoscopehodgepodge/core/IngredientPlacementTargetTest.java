package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientPlacementTargetTest {
    private static final BlockPos ORIGIN = BlockPos.ZERO;
    /** 单方块容器：摆放空间与方块自身重合。 */
    private static final PlacementSpace.Bounds SINGLE = new PlacementSpace.Bounds(0, 16, 0, 16, 12);
    /** 中瓷盘左半格：摆放空间横跨两格，x 可达 32 像素。 */
    private static final PlacementSpace.Bounds MEDIAN_LEFT = new PlacementSpace.Bounds(0, 32, 0, 16, 12);

    @Test
    void topFaceHitStacksRegardlessOfViewAngle() {
        PlacedIngredient cube = iceCube(8, 8);
        List<PlacedIngredient> existing = List.of(cube);

        IngredientPlacementTarget.Pixel steep = resolveTopFace(existing, SINGLE,
                new Vec3(0.5, 1.6, 0.2), 0.5, 0.5, topOf(cube));
        IngredientPlacementTarget.Pixel shallow = resolveTopFace(existing, SINGLE,
                new Vec3(-10.0, 3.0, 0.5), 0.5, 0.5, topOf(cube));

        assertEquals(steep, shallow, "视距与俯角不应影响顶面命中结果");
        assertEquals(8, shallow.x());
        assertEquals(8, shallow.z());
        assertStackedOn(existing, shallow, SINGLE, cube);
    }

    @Test
    void everyPointOnTopFaceStaysInsideStackColumn() {
        PlacedIngredient cube = iceCube(8, 8);
        List<PlacedIngredient> existing = List.of(cube);
        // 3 像素模型的顶面横跨 6.5..9.5 像素，半像素边带曾因向下取整落到 6 而无法堆叠。
        double[] samples = {6.5, 6.6, 7.0, 7.4, 8.0, 8.7, 9.0, 9.4, 9.5};

        for (double pixelOnFace : samples) {
            IngredientPlacementTarget.Pixel target = resolveTopFace(existing, SINGLE,
                    new Vec3(0.5, 1.6, 0.5), pixelOnFace / 16.0, 0.5, topOf(cube));
            assertTrue(2 * target.x() >= cube.xMin() && 2 * target.x() < cube.xMax(),
                    "顶面像素 " + pixelOnFace + " 投影到了非法列 " + target.x());
            assertStackedOn(existing, target, SINGLE, cube);
        }
    }

    @Test
    void topFaceHitAcrossPartSeamKeepsNeighbourColumn() {
        // 冰块位于右半格（左半格坐标系下 x = 24），香草射线在远距离会把命中方块判成左半格。
        PlacedIngredient cube = iceCube(24, 8);
        List<PlacedIngredient> existing = List.of(cube);

        IngredientPlacementTarget.Pixel target = resolveTopFace(existing, MEDIAN_LEFT,
                new Vec3(-8.0, 2.5, 0.5), 1.5, 0.5, topOf(cube));

        assertEquals(24, target.x(), "跨接缝的顶面命中被钳制到了方块边界");
        assertEquals(8, target.z());
        assertStackedOn(existing, target, MEDIAN_LEFT, cube);
    }

    @Test
    void sideHitAcrossPartSeamDoesNotClampFreeAxis() {
        PlacedIngredient cube = iceCube(24, 8);
        List<PlacedIngredient> existing = List.of(cube);
        BlockHitResult hit = new BlockHitResult(new Vec3(1.5, 3.0 / 16.0, cube.zMin() / 32.0),
                Direction.NORTH, ORIGIN, false);

        IngredientPlacementTarget.Pixel target = IngredientPlacementTarget.resolve(existing, ORIGIN,
                new Vec3(1.5, 3.0 / 16.0, -8.0), hit, PackingIngredients.RED_BERRY, 0,
                MEDIAN_LEFT, false).orElseThrow();

        assertEquals(24, target.x(), "侧面命中的自由轴被钳制到了方块边界");
        assertEquals(Math.floorDiv(cube.zMin() - PackingIngredients.RED_BERRY.getSize().z(), 2),
                target.z());
    }

    @Test
    void containerFloorHitProjectsToNearestPixelGrid() {
        BlockHitResult hit = new BlockHitResult(new Vec3(10.9 / 16.0, 2.0 / 16.0, 5.2 / 16.0),
                Direction.UP, ORIGIN, false);

        IngredientPlacementTarget.Pixel target = IngredientPlacementTarget.resolve(List.of(), ORIGIN,
                new Vec3(0.5, 2.0, 0.5), hit, PackingIngredients.RED_BERRY, 0, SINGLE, false)
                .orElseThrow();

        assertEquals(11, target.x());
        assertEquals(5, target.z());
    }

    @Test
    void bottomFaceIsRejected() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.5, 0.0, 0.5), Direction.DOWN, ORIGIN, false);

        assertTrue(IngredientPlacementTarget.resolve(List.of(), ORIGIN, new Vec3(0.5, -2.0, 0.5), hit,
                PackingIngredients.RED_BERRY, 0, SINGLE, false).isEmpty());
    }

    private static PlacedIngredient iceCube(int x, int z) {
        PackingIngredients.Size size = PackingIngredients.ICE_CUBE.getSize();
        return new PlacedIngredient(PackingIngredients.ICE_CUBE.getId(), x, 2, z,
                size.x(), size.y(), size.z());
    }

    private static double topOf(PlacedIngredient ingredient) {
        return (ingredient.y() + ingredient.sizeY()) / 16.0;
    }

    private static IngredientPlacementTarget.Pixel resolveTopFace(List<PlacedIngredient> existing,
                                                                  PlacementSpace.Bounds bounds, Vec3 eye,
                                                                  double localX, double localZ, double localY) {
        BlockHitResult hit = new BlockHitResult(new Vec3(localX, localY, localZ),
                Direction.UP, ORIGIN, false);
        return IngredientPlacementTarget.resolve(existing, ORIGIN, eye, hit,
                PackingIngredients.RED_BERRY, 0, bounds, false).orElseThrow();
    }

    private static void assertStackedOn(List<PlacedIngredient> existing,
                                        IngredientPlacementTarget.Pixel target,
                                        PlacementSpace.Bounds bounds, PlacedIngredient below) {
        PlacementSpace.Result result = PlacementSpace.place(existing, PackingIngredients.RED_BERRY,
                target.x(), target.z(), 40, 2, bounds, 0);
        assertTrue(result.success(), "顶面放置被拒绝：" + result.failure());
        assertEquals(below.y() + below.sizeY(), result.placement().orElseThrow().y(),
                "模型没有叠放在目标顶面之上");
    }
}
