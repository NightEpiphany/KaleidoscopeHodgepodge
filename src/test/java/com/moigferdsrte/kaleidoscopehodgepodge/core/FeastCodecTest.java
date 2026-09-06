package com.moigferdsrte.kaleidoscopehodgepodge.core;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeastCodecTest {

    private static final Identifier ID_A = Identifier.fromNamespaceAndPath("kaleidoscope_hodgepodge", "red_berry");
    private static final Identifier ID_B = Identifier.fromNamespaceAndPath("kaleidoscope_hodgepodge", "mutton");

    @Test
    void roundTripsDishWithoutEffects() throws Exception {
        CustomFeastData original = new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH, List.of(
                new PlacedIngredient(ID_A, 8, 2, 8, 2, 2, 2, 0,
                        new IngredientFoodData(4, 0.4F, List.of())),
                new PlacedIngredient(ID_B, 6, 6, 10, 10, 2, 6, 2,
                        new IngredientFoodData(3, 0.5F, List.of()))));

        String code = FeastCodec.encode("kaleidoscope_hodgepodge:wooden_plate", original);
        FeastCodec.Decoded decoded = FeastCodec.decode(code);

        assertEquals("kaleidoscope_hodgepodge:wooden_plate", decoded.containerPath());
        assertEquals(CustomFeastData.ContainerKind.DISH, decoded.feast().kind());
        assertEquals(Direction.NORTH, decoded.feast().facing());
        assertEquals(2, decoded.feast().ingredients().size());

        PlacedIngredient first = decoded.feast().ingredients().get(0);
        assertEquals(ID_A, first.id());
        assertEquals(8, first.x());
        assertEquals(2, first.y());
        assertEquals(8, first.z());
        assertEquals(2, first.sizeX());
        assertEquals(2, first.sizeY());
        assertEquals(2, first.sizeZ());
        assertEquals(0, first.rotation());
        assertEquals(4, first.food().nutrition());
        assertEquals(0.4F, first.food().saturation(), 1.0E-6F);
        assertTrue(first.food().effects().isEmpty());

        PlacedIngredient second = decoded.feast().ingredients().get(1);
        assertEquals(ID_B, second.id());
        assertEquals(6, second.x());
        assertEquals(6, second.y());
        assertEquals(10, second.z());
        assertEquals(10, second.sizeX());
        assertEquals(2, second.sizeY());
        assertEquals(6, second.sizeZ());
        assertEquals(2, second.rotation());
    }

    @Test
    void roundTripsSoupWithEffects() throws Exception {
        IngredientStatusEffect effectA = new IngredientStatusEffect(
                Identifier.fromNamespaceAndPath("minecraft", "regeneration"), 200, 1, true, true, true);
        IngredientStatusEffect effectB = new IngredientStatusEffect(
                Identifier.fromNamespaceAndPath("minecraft", "speed"), 400, 0, false, true, false);
        IngredientEffectGroup group = new IngredientEffectGroup(0.75F, List.of(effectA, effectB));
        CustomFeastData original = new CustomFeastData(
                CustomFeastData.ContainerKind.SOUP, Direction.EAST, List.of(
                new PlacedIngredient(ID_A, 4, 4, 4, 4, 4, 4, 1,
                        new IngredientFoodData(2, 0.8F, List.of(group)))));

        String code = FeastCodec.encode("kaleidoscope_hodgepodge:porcelain_soup_bowl", original);
        FeastCodec.Decoded decoded = FeastCodec.decode(code);

        assertEquals(CustomFeastData.ContainerKind.SOUP, decoded.feast().kind());
        assertEquals(Direction.EAST, decoded.feast().facing());
        PlacedIngredient ingredient = decoded.feast().ingredients().get(0);
        assertEquals(1, ingredient.rotation());
        assertEquals(2, ingredient.food().nutrition());
        assertEquals(0.8F, ingredient.food().saturation(), 1.0E-6F);
        assertEquals(1, ingredient.food().effects().size());

        IngredientEffectGroup parsedGroup = ingredient.food().effects().get(0);
        assertEquals(0.75F, parsedGroup.probability(), 1.0E-6F);
        assertEquals(2, parsedGroup.effects().size());
        assertEquals(Identifier.fromNamespaceAndPath("minecraft", "regeneration"),
                parsedGroup.effects().get(0).id());
        assertEquals(200, parsedGroup.effects().get(0).duration());
        assertEquals(1, parsedGroup.effects().get(0).amplifier());
        assertTrue(parsedGroup.effects().get(0).ambient());
        assertEquals(Identifier.fromNamespaceAndPath("minecraft", "speed"),
                parsedGroup.effects().get(1).id());
        assertEquals(400, parsedGroup.effects().get(1).duration());
        assertEquals(0, parsedGroup.effects().get(1).amplifier());
        assertTrue(parsedGroup.effects().get(1).visible());
    }

    @Test
    void preservesIngredientOrderRoundTrippedThroughCodec() throws Exception {
        List<PlacedIngredient> unsorted = List.of(
                new PlacedIngredient(ID_B, 8, 8, 8, 4, 2, 4),
                new PlacedIngredient(ID_A, 8, 2, 8, 2, 2, 2));
        CustomFeastData original = new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH, unsorted);

        FeastCodec.Decoded decoded = FeastCodec.decode(
                FeastCodec.encode("kaleidoscope_hodgepodge:porcelain_plate", original));

        assertEquals(ID_B, decoded.feast().ingredients().get(0).id());
        assertEquals(ID_A, decoded.feast().ingredients().get(1).id());
    }

    @Test
    void rejectsUnsupportedVersion() {
        String code = "KHP:99|kaleidoscope_hodgepodge:wooden_plate|d|n|0";
        FeastCodec.FormatException error = assertThrows(FeastCodec.FormatException.class,
                () -> FeastCodec.decode(code));
        assertTrue(error.getMessage().contains("version"));
    }

    @Test
    void rejectsMissingHeaderAndCountMismatch() {
        assertThrows(FeastCodec.FormatException.class, () -> FeastCodec.decode("KHP:1"));
        String code = "KHP:1|kaleidoscope_hodgepodge:wooden_plate|d|n|2|"
                + "a;1;1;1;1;1;1;0;0;0.0";
        FeastCodec.FormatException error = assertThrows(FeastCodec.FormatException.class,
                () -> FeastCodec.decode(code));
        assertTrue(error.getMessage().contains("mismatch"));
    }

    @Test
    void rejectsMalformedIngredient() {
        String code = "KHP:1|kaleidoscope_hodgepodge:wooden_plate|d|n|1|"
                + "not_a_path;1;1;1;1;1;1;0;0;0.0";
        assertThrows(FeastCodec.FormatException.class, () -> FeastCodec.decode(code));
    }
}
