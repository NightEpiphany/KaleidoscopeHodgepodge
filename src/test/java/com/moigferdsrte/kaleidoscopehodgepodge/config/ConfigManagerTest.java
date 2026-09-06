package com.moigferdsrte.kaleidoscopehodgepodge.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigManagerTest {
    @AfterEach
    void resetConfig() {
        GeneralConfig.reset();
    }

    @Test
    void interactionDefaultsMatchPlayerFacingBehavior() {
        GeneralConfig.Snapshot config = GeneralConfig.snapshot();
        assertTrue(config.modelMicroOffset());
        assertTrue(config.placementAnimation());
        assertEquals(0.4D, config.placementPreviewAlpha());
        assertFalse(config.allowHandheldDishEating());
        assertTrue(config.allowHandheldSoupEating());
        assertTrue(config.ingredientModelCollision());
    }

    @Test
    void ingredientCollisionCanBeDisabledWithoutChangingOtherOptions() {
        GeneralConfig.Snapshot original = GeneralConfig.snapshot();
        GeneralConfig.Snapshot disabled = original.withIngredientModelCollision(false);

        assertFalse(disabled.ingredientModelCollision());
        assertEquals(original.porcelainCapacity(), disabled.porcelainCapacity());
        assertEquals(original.placementPreviewAlpha(), disabled.placementPreviewAlpha());
    }
}
