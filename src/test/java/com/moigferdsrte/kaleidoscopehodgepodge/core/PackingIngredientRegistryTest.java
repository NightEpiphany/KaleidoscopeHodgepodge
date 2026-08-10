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
        assertEquals(List.of(PackingIngredients.RED_BERRY, PackingIngredients.ARDENT_CORE,
                        PackingIngredients.BLAZE_ROD, PackingIngredients.MUTTON),
                PackingIngredientRegistry.bySource(Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop")));
    }

    @Test
    void sharedModelsAreIndexedForEverySource() {
        List<Identifier> sashimiSources = List.of(
                cookeryId("cold_style_sashimi"), cookeryId("desert_style_sashimi"),
                cookeryId("end_style_sashimi"), cookeryId("nether_style_sashimi"),
                cookeryId("tundra_style_sashimi"));

        assertEquals(sashimiSources, PackingIngredients.SASHIMI.getSrcFoodIds());
        assertEquals("packing_ingredients/common/sashimi", PackingIngredients.SASHIMI.getResourceLoc());
        assertEquals(4, PackingIngredients.SASHIMI.getCountPerDish(cookeryId("cold_style_sashimi")));
        assertEquals(4, PackingIngredients.SASHIMI.getCountPerDish(cookeryId("tundra_style_sashimi")));
        sashimiSources.forEach(source -> assertTrue(
                PackingIngredientRegistry.bySource(source).contains(PackingIngredients.SASHIMI)));
        assertEquals(sashimiSources, PackingIngredientRegistry.sourceIdsFor(List.of(
                PackingIngredients.SASHIMI.getId(), PackingIngredients.SASHIMI.getId())));
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

    private static Identifier cookeryId(String path) {
        return Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, path);
    }
}
