package com.moigferdsrte.kaleidoscopehodgepodge.client.animation;

import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngredientPreviewPositionAnimationTest {
    private static final BlockPos POS = BlockPos.ZERO;

    @Test
    void pixelMovementUsesAShortSmoothTransition() {
        IngredientPreviewPositionAnimation animation = new IngredientPreviewPositionAnimation();
        long start = 1_000_000_000L;
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1,
                4, 2, 6, start);
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1,
                5, 3, 7, start + 1_000_000L);
        animation.sample(start + 21_000_000L);

        assertEquals(4.5F, animation.x(), 0.02F);
        assertEquals(2.5F, animation.y(), 0.02F);
        assertEquals(6.5F, animation.z(), 0.02F);
        animation.sample(start + 150_000_000L);
        assertEquals(5.0F, animation.x());
        assertEquals(3.0F, animation.y());
        assertEquals(7.0F, animation.z());
    }

    @Test
    void rapidlyChangingTargetContinuesFromCurrentPosition() {
        IngredientPreviewPositionAnimation animation = new IngredientPreviewPositionAnimation();
        long start = 2_000_000_000L;
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1,
                0, 0, 0, start);
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1,
                1, 0, 0, start + 1_000_000L);
        animation.sample(start + 51_000_000L);
        float current = animation.x();
        animation.beginFrame();
        animation.update(POS, PackingIngredients.RED_BERRY.getId(), 1,
                2, 0, 0, start + 51_000_000L);
        animation.sample(start + 51_000_000L);

        assertEquals(current, animation.x(), 0.001F);
    }
}
