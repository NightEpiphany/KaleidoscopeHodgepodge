package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientCollisionHeightMapTest {
    @Test
    void buildsOneEightPixelColumnAtTheTallestIngredientHeight() {
        VoxelShape shape = IngredientCollisionHeightMap.fromIngredients(List.of(
                ingredient(4, 5, 4, 2, 3, 2),
                ingredient(4, 11, 4, 2, 4, 2)));

        assertEquals(1, shape.toAabbs().size());
        assertBox(shape.toAabbs().getFirst(), 0.0D, 0.5D, 0.0D, 0.5D, 15.0D / 16.0D);
    }

    @Test
    void ingredientCrossingQuarterBoundariesRaisesEveryTouchedColumn() {
        VoxelShape shape = IngredientCollisionHeightMap.fromIngredients(List.of(
                ingredient(8, 6, 8, 4, 4, 4)));

        assertCoveredAtHeight(shape, 0.25D, 0.25D, 10.0D / 16.0D);
        assertCoveredAtHeight(shape, 0.75D, 0.25D, 10.0D / 16.0D);
        assertCoveredAtHeight(shape, 0.25D, 0.75D, 10.0D / 16.0D);
        assertCoveredAtHeight(shape, 0.75D, 0.75D, 10.0D / 16.0D);
    }

    @Test
    void ignoresIngredientsOutsideThisBlockAndReturnsEmptyForNoOverlap() {
        VoxelShape shape = IngredientCollisionHeightMap.fromIngredients(List.of(
                ingredient(-4, 4, -4, 2, 2, 2),
                ingredient(20, 4, 20, 2, 2, 2)));

        assertTrue(shape.isEmpty());
    }

    private static PlacedIngredient ingredient(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        return new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), x, y, z, sizeX, sizeY, sizeZ);
    }

    private static void assertBox(AABB box, double minX, double maxX, double minZ, double maxZ, double maxY) {
        assertEquals(minX, box.minX);
        assertEquals(maxX, box.maxX);
        assertEquals(minZ, box.minZ);
        assertEquals(maxZ, box.maxZ);
        assertEquals(0.0D, box.minY);
        assertEquals(maxY, box.maxY);
    }

    private static void assertCoveredAtHeight(VoxelShape shape, double x, double z, double height) {
        assertTrue(shape.toAabbs().stream().anyMatch(box -> box.minX <= x && box.maxX >= x
                        && box.minZ <= z && box.maxZ >= z && box.minY == 0.0D && box.maxY >= height),
                "Expected collision coverage at " + x + "," + z);
    }
}
