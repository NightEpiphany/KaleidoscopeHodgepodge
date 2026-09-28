package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TeaTrayModelResourcesTest {
    private static final String ASSETS = "assets/kaleidoscope_hodgepodge/";

    @Test
    void everyTeaModelParsesAndHasItsItemDefinitionAndTextures() throws IOException {
        for (String tea : List.of("barley_tea", "biluochun", "butter_tea", "flower_tea",
                "mystery_tea", "oolong", "sakura_fubuki", "tieguanyin", "empty_cup")) {
            String modelPath = ASSETS + "models/block/tea_cups/" + tea + ".json";
            JsonObject definition = json(ASSETS + "items/tea_cups/" + tea + ".json").getAsJsonObject("model");
            assertEquals("minecraft:model", definition.get("type").getAsString());
            assertEquals("kaleidoscope_hodgepodge:block/tea_cups/" + tea, definition.get("model").getAsString());
            try (var reader = new InputStreamReader(resource(modelPath), StandardCharsets.UTF_8)) {
                assertNotNull(CuboidModel.fromStream(reader).geometry());
            }
            for (var entry : json(modelPath).getAsJsonObject("textures").entrySet()) {
                String[] location = entry.getValue().getAsString().split(":", 2);
                try (InputStream texture = resource("assets/" + location[0] + "/textures/" + location[1] + ".png")) {
                    assertNotNull(texture);
                }
            }
        }
    }

    @Test
    void trayGeometryIsFourteenByFourteenAndOnePixelHigh() throws IOException {
        JsonObject tray = json(ASSETS + "models/block/tea_tray.json");
        JsonObject element = tray.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(JsonParser.parseString("[1,0,1]"), element.get("from"));
        assertEquals(JsonParser.parseString("[15,1,15]"), element.get("to"));
        try (var reader = new InputStreamReader(resource(ASSETS + "models/block/tea_tray.json"), StandardCharsets.UTF_8)) {
            assertNotNull(CuboidModel.fromStream(reader).geometry());
        }
    }

    private static JsonObject json(String path) throws IOException {
        try (var reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream resource = TeaTrayModelResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(resource, path);
        return resource;
    }
}
