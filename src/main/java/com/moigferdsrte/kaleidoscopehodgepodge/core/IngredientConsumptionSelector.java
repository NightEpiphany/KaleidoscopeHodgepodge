package com.moigferdsrte.kaleidoscopehodgepodge.core;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.OptionalInt;

/** Selects an ingredient from the highest occupied base layer. */
public final class IngredientConsumptionSelector {
    public static OptionalInt highest(List<PlacedIngredient> ingredients, RandomSource random) {
        int highestY = Integer.MIN_VALUE;
        int selected = -1;
        int candidates = 0;
        for (int index = 0; index < ingredients.size(); index++) {
            int y = ingredients.get(index).y();
            if (y > highestY) {
                highestY = y;
                selected = index;
                candidates = 1;
            } else if (y == highestY && random.nextInt(++candidates) == 0) {
                // 蓄水池抽样保证同一高度的材料等概率被选中，且无需额外列表。
                selected = index;
            }
        }
        return selected < 0 ? OptionalInt.empty() : OptionalInt.of(selected);
    }

    private IngredientConsumptionSelector() {
    }
}
