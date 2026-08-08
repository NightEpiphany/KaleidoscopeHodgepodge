package com.moigferdsrte.kaleidoscopehodgepodge.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigManagerTest {
    @Test
    void removesOnlyTrailingCommas() {
        String source = "{\"text\":\",}\",\"values\":[1,2,],// comment\n\"enabled\":true,}";
        var parsed = JsonParser.parseString(ConfigManager.stripTrailingCommas(source)).getAsJsonObject();
        assertEquals(",}", parsed.get("text").getAsString());
        assertEquals(2, parsed.getAsJsonArray("values").size());
    }
}
