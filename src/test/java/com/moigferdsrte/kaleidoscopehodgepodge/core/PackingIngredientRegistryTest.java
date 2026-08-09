package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.mojang.serialization.JsonOps;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PackingIngredientRegistryTest {
    @Test
    void indexesIdsAndOrderedSources() {
        assertEquals(PackingIngredients.RED_BERRY,
                PackingIngredientRegistry.byId(PackingIngredients.RED_BERRY.getId()).orElseThrow());
        assertEquals(List.of(PackingIngredients.RED_BERRY, PackingIngredients.MUTTON,
                        PackingIngredients.ARDENT_CORE, PackingIngredients.BLAZE_ROD),
                PackingIngredientRegistry.bySource(Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop")));
    }

    @Test
    void feastCodecRoundTripsPlacementSnapshot() {
        IngredientFoodData food = new IngredientFoodData(2, 1.6F, List.of(
                new IngredientEffectGroup(1.0F, List.of(new IngredientStatusEffect(
                        Identifier.withDefaultNamespace("haste"), 1200, 0, false, true, true)))));
        CustomFeastData source = new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH,
                List.of(new PlacedIngredient(PackingIngredients.RED_BERRY.getId(),
                        8, 2, 8, 2, 2, 2, 3, food)));
        var encoded = CustomFeastData.CODEC.encodeStart(JsonOps.INSTANCE, source).getOrThrow();
        CustomFeastData decoded = CustomFeastData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(source, decoded);
    }

    @Test
    void suitabilityMetadataMatchesDeclaredContainers() {
        assertEquals(PackingIngredients.SuitableFor.BOTH, PackingIngredients.RED_BERRY.suitableFor());
        assertEquals(PackingIngredients.SuitableFor.DISH, PackingIngredients.MUTTON.suitableFor());
    }

    @Test
    void wholeDishCountsAreExpandedAndCapped() {
        PackingBagContents blazeLambChop = PackingBagService.fromWholeDish(
                PackingIngredientRegistry.bySource(
                        Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop")));
        assertEquals(8, blazeLambChop.ingredients().size());
        assertEquals(1, blazeLambChop.ingredients().stream()
                .filter(value -> value.id().equals(PackingIngredients.RED_BERRY.getId())).count());
        assertEquals(2, blazeLambChop.ingredients().stream()
                .filter(value -> value.id().equals(PackingIngredients.ARDENT_CORE.getId())).count());
        assertEquals(4, blazeLambChop.ingredients().stream()
                .filter(value -> value.id().equals(PackingIngredients.BLAZE_ROD.getId())).count());

        PackingBagContents oversized = new PackingBagContents(Collections.nCopies(12,
                new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        assertEquals(PackingBagContents.MAX_INGREDIENTS, oversized.ingredients().size());
    }
}
