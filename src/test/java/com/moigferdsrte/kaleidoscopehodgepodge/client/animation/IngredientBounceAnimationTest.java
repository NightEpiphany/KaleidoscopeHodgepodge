package com.moigferdsrte.kaleidoscopehodgepodge.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientBounceAnimationTest {
    @Test
    void startsCompressedAndSettlesAtIdentity() {
        var initial = IngredientBounceAnimation.sample(0L);
        assertEquals(0.75F, initial.vertical(), 0.0001F);
        assertTrue(initial.horizontal() > 1.0F);

        var settled = IngredientBounceAnimation.sample(IngredientBounceAnimation.DURATION_MILLIS);
        assertEquals(1.0F, settled.vertical(), 0.0001F);
        assertEquals(1.0F, settled.horizontal(), 0.0001F);
    }

    @Test
    void reboundsWithDecreasingAmplitude() {
        float firstPeak = IngredientBounceAnimation.sample(150L).vertical();
        float secondCompression = IngredientBounceAnimation.sample(300L).vertical();
        float secondPeak = IngredientBounceAnimation.sample(450L).vertical();

        assertTrue(firstPeak > 1.0F);
        assertTrue(secondCompression < 1.0F);
        assertTrue(secondPeak > 1.0F);
        assertTrue(Math.abs(secondPeak - 1.0F) < Math.abs(firstPeak - 1.0F));
    }
}
