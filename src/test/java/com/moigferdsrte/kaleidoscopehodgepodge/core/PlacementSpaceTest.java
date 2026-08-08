package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlacementSpaceTest {
    @AfterEach
    void resetConfig() {
        GeneralConfig.reset();
    }

    @Test
    void placesAtConfiguredDishHeight() {
        PlacementSpace.Result result = PlacementSpace.place(List.of(), PackingIngredients.RED_BERRY,
                8, 8, CustomFeastData.ContainerKind.DISH);
        assertTrue(result.success());
        assertEquals(2, result.placement().orElseThrow().y());
    }

    @Test
    void stacksOnHighestIntersectedColumn() {
        List<PlacedIngredient> placed = new ArrayList<>();
        placed.add(new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 2, 2, 2));
        placed.add(new PlacedIngredient(PackingIngredients.ARDENT_CORE.getId(), 8, 4, 8, 3, 3, 3));
        PlacementSpace.Result result = PlacementSpace.place(placed, PackingIngredients.RED_BERRY,
                8, 8, CustomFeastData.ContainerKind.DISH);
        assertTrue(result.success());
        assertEquals(7, result.placement().orElseThrow().y());
    }

    @Test
    void rejectsOutOfBoundsAndCapacity() {
        assertEquals(PlacementSpace.Failure.OUT_OF_BOUNDS,
                PlacementSpace.place(List.of(), PackingIngredients.MUTTON, 0, 8,
                        CustomFeastData.ContainerKind.DISH).failure());
        List<PlacedIngredient> full = java.util.Collections.nCopies(12,
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 1, 1, 1));
        assertEquals(PlacementSpace.Failure.CAPACITY,
                PlacementSpace.place(full, PackingIngredients.RED_BERRY, 8, 8,
                        CustomFeastData.ContainerKind.DISH).failure());
    }

    @Test
    void detectsThreeDimensionalOverlap() {
        PlacedIngredient first = new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 4, 4, 4);
        PlacedIngredient second = new PlacedIngredient(PackingIngredients.MUTTON.getId(), 9, 3, 8, 4, 4, 4);
        assertTrue(first.intersects(second));
    }
}
