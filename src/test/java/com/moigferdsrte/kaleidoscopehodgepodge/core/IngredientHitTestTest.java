package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientHitTestTest {
    private static final BlockPos ORIGIN = BlockPos.ZERO;

    @Test
    void selectsNearestIngredientAlongRay() {
        PlacedIngredient lower = new PlacedIngredient(
                PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 4, 2, 4);
        PlacedIngredient upper = new PlacedIngredient(
                PackingIngredients.ARDENT_CORE.getId(), 8, 4, 8, 4, 3, 4);

        var selected = IngredientHitTest.nearest(List.of(lower, upper), ORIGIN,
                new Vec3(0.5, 2.0, 0.5), new Vec3(0.5, 0.0, 0.5));

        assertTrue(selected.isPresent());
        assertEquals(1, selected.getAsInt());
    }

    @Test
    void missesIngredientsOutsideRay() {
        PlacedIngredient ingredient = new PlacedIngredient(
                PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 2, 2, 2);

        assertTrue(IngredientHitTest.nearest(List.of(ingredient), ORIGIN,
                new Vec3(0.1, 2.0, 0.1), new Vec3(0.1, 0.0, 0.1)).isEmpty());
    }

    @Test
    void previewRotationUsesTheUnrotatedFootprint() {
        PlacedIngredient rotated = new PlacedIngredient(
                PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 2, 2, 6, 1);

        AABB shape = IngredientHitTest.localShapeBeforeRotation(rotated).bounds();

        assertEquals(12.0D / 32.0D, shape.getXsize());
        assertEquals(4.0D / 32.0D, shape.getZsize());
    }
}
