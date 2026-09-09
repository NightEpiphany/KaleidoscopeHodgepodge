package com.moigferdsrte.kaleidoscopehodgepodge.client.animation;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientPreviewRotationAnimationTest {
    private static final BlockPos POS = BlockPos.ZERO;

    @Test
    void newPreviewSnapsAndRotationChangeEasesClockwise() {
        IngredientPreviewRotationAnimation animation = new IngredientPreviewRotationAnimation();
        long start = 1_000_000_000L;
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1, 0, start);
        assertEquals(0.0F, animation.sample(start));

        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1, 1, start + 1_000_000L);
        float halfway = animation.sample(start + 121_000_000L);
        assertTrue(halfway < 0.0F && halfway > -90.0F,
                "preview should rotate clockwise toward -90 degrees");
        assertEquals(-90.0F, animation.sample(start + 300_000_000L));
    }

    @Test
    void newContentDoesNotContinueThePreviousModelRotation() {
        IngredientPreviewRotationAnimation animation = new IngredientPreviewRotationAnimation();
        long now = 4_000_000_000L;
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1, 3, now);
        animation.beginFrame();
        animation.update(POS, PackingIngredients.MUTTON.getId(), 2, 0, now + 50_000_000L);
        assertEquals(0.0F, animation.sample(now + 50_000_000L));
    }

    @Test
    void clockwiseWrapAroundUsesTheShortQuarterTurn() {
        IngredientPreviewRotationAnimation animation = new IngredientPreviewRotationAnimation();
        long now = 7_000_000_000L;
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1, 3, now);
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1, 0, now + 1_000_000L);
        assertEquals(-360.0F, animation.sample(now + 300_000_000L));
    }
}
