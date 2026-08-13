package com.moigferdsrte.kaleidoscopehodgepodge.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(config.allowHandheldFeastEating());
    }
}
