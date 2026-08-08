package com.moigferdsrte.kaleidoscopehodgepodge.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public final class ConfigManager {
    private static final String FILE_NAME = "kaleidoscope_hodgepodge.json5";
    private static volatile boolean started;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static synchronized void start() {
        if (started) return;
        started = true;
        ensureFile();
        reload();
        Thread watcher = new Thread(ConfigManager::watch, "Kaleidoscope Hodgepodge Config Watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    public static boolean reload() {
        try {
            String source = Files.readString(path(), StandardCharsets.UTF_8);
            JsonReader reader = new JsonReader(new StringReader(stripTrailingCommas(source)));
            reader.setStrictness(Strictness.LENIENT);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            GeneralConfig.Snapshot previous = GeneralConfig.snapshot();
            GeneralConfig.Snapshot loaded = new GeneralConfig.Snapshot(
                    integer(root, "dish_capacity", previous.dishCapacity(), 1, 12),
                    integer(root, "soup_capacity", previous.soupCapacity(), 1, 9),
                    integer(root, "dish_base_height", previous.dishBaseHeight(), 0, 15),
                    integer(root, "soup_base_height", previous.soupBaseHeight(), 0, 15),
                    integer(root, "max_model_height", previous.maxModelHeight(), 1, 16),
                    bool(root, "debug_logging", previous.debugLogging())
            );
            GeneralConfig.replace(loaded);
            KaleidoscopeHodgepodge.LOGGER.info("Reloaded {}: {}", FILE_NAME, describe());
            return true;
        } catch (Exception exception) {
            KaleidoscopeHodgepodge.LOGGER.error("Could not reload {}; keeping the last valid configuration", FILE_NAME, exception);
            return false;
        }
    }

    public static String describe() {
        GeneralConfig.Snapshot value = GeneralConfig.snapshot();
        return "dishCapacity=" + value.dishCapacity() + ", soupCapacity=" + value.soupCapacity()
                + ", dishBaseHeight=" + value.dishBaseHeight() + ", soupBaseHeight=" + value.soupBaseHeight()
                + ", maxModelHeight=" + value.maxModelHeight() + ", debug=" + value.debugLogging();
    }

    private static int integer(JsonObject root, String key, int fallback, int min, int max) {
        if (!root.has(key)) return fallback;
        return Math.max(min, Math.min(max, root.get(key).getAsInt()));
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }

    static String stripTrailingCommas(String source) {
        StringBuilder output = new StringBuilder(source.length());
        boolean string = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (string) {
                output.append(current);
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                else if (current == '"') string = false;
                continue;
            }
            if (current == '"') {
                string = true;
                output.append(current);
                continue;
            }
            if (current == ',' && followedByClosingToken(source, i + 1)) continue;
            output.append(current);
        }
        return output.toString();
    }

    private static boolean followedByClosingToken(String source, int start) {
        for (int i = start; i < source.length(); i++) {
            char current = source.charAt(i);
            if (Character.isWhitespace(current)) continue;
            if (current == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                i += 2;
                while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') i++;
                continue;
            }
            if (current == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < source.length() && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) i++;
                i++;
                continue;
            }
            return current == '}' || current == ']';
        }
        return false;
    }

    private static void ensureFile() {
        Path path = path();
        if (Files.exists(path)) return;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, """
                    {
                      // Ingredient limits. Values above the hard maximum are clamped.
                      \"dish_capacity\": 12,
                      \"soup_capacity\": 9,
                      // Block-local model floor, measured in pixels.
                      \"dish_base_height\": 2,
                      \"soup_base_height\": 4,
                      \"max_model_height\": 16,
                      \"debug_logging\": false,
                    }
                    """, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            KaleidoscopeHodgepodge.LOGGER.error("Could not create default configuration {}", path, exception);
        }
    }

    private static void watch() {
        Path path = path();
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            path.getParent().register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = service.take();
                boolean changed = key.pollEvents().stream().anyMatch(event -> path.getFileName().equals(event.context()));
                key.reset();
                if (changed) reload();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            KaleidoscopeHodgepodge.LOGGER.error("Configuration watcher stopped unexpectedly", exception);
        }
    }

    private ConfigManager() {}
}
