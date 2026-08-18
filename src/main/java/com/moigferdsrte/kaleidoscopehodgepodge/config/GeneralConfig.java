package com.moigferdsrte.kaleidoscopehodgepodge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.concurrent.atomic.AtomicReference;

public final class GeneralConfig {
    static final ModConfigSpec COMMON_SPEC;
    static final ModConfigSpec CLIENT_SPEC;

    private static final ModConfigSpec.IntValue WOODEN_PLATE_CAPACITY;
    private static final ModConfigSpec.IntValue PORCELAIN_CAPACITY;
    private static final ModConfigSpec.IntValue SOUP_CAPACITY;
    private static final ModConfigSpec.IntValue DISH_BASE_HEIGHT;
    private static final ModConfigSpec.IntValue SOUP_BASE_HEIGHT;
    private static final ModConfigSpec.IntValue WOODEN_MAX_MODEL_HEIGHT;
    private static final ModConfigSpec.IntValue PORCELAIN_MAX_MODEL_HEIGHT;
    private static final ModConfigSpec.BooleanValue ALLOW_HANDHELD_FEAST_EATING;
    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    private static final ModConfigSpec.BooleanValue MODEL_MICRO_OFFSET;
    private static final ModConfigSpec.BooleanValue PLACEMENT_ANIMATION;
    private static final ModConfigSpec.DoubleValue PLACEMENT_PREVIEW_ALPHA;

    private static final Snapshot DEFAULT = new Snapshot(
            20, 40, 40, 2, 4, 16, 32,
            false, false, true, true, 0.4);
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(DEFAULT);

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.push("containers");
        WOODEN_PLATE_CAPACITY = common.comment("Maximum ingredient models on a wooden plate.")
                .defineInRange("woodenPlateCapacity", DEFAULT.woodenPlateCapacity(), 1, 20);
        PORCELAIN_CAPACITY = common.comment("Base capacity of porcelain containers.")
                .defineInRange("porcelainCapacity", DEFAULT.porcelainCapacity(), 1, 40);
        SOUP_CAPACITY = common.comment("Maximum ingredient models in a porcelain soup bowl.")
                .defineInRange("soupCapacity", DEFAULT.soupCapacity(), 1, 40);
        DISH_BASE_HEIGHT = common.comment("Dish model floor in pixels.")
                .defineInRange("dishBaseHeight", DEFAULT.dishBaseHeight(), 0, 15);
        SOUP_BASE_HEIGHT = common.comment("Soup model floor in pixels.")
                .defineInRange("soupBaseHeight", DEFAULT.soupBaseHeight(), 0, 15);
        WOODEN_MAX_MODEL_HEIGHT = common.comment("Wooden plate placement height in pixels.")
                .defineInRange("woodenMaxModelHeight", DEFAULT.woodenMaxModelHeight(), 1, 16);
        PORCELAIN_MAX_MODEL_HEIGHT = common.comment("Porcelain placement height in pixels.")
                .defineInRange("porcelainMaxModelHeight", DEFAULT.porcelainMaxModelHeight(), 1, 32);
        common.pop().push("gameplay");
        ALLOW_HANDHELD_FEAST_EATING = common.comment(
                        "Allow a filled custom feast item to be eaten directly from the player's hand.")
                .define("allowHandheldFeastEating", DEFAULT.allowHandheldFeastEating());
        DEBUG_LOGGING = common.comment("Write additional ingredient interaction diagnostics.")
                .define("debugLogging", DEFAULT.debugLogging());
        common.pop();
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("rendering");
        MODEL_MICRO_OFFSET = client.comment(
                        "Apply stable sub-pixel offsets to ingredient models to prevent z-fighting.")
                .define("modelMicroOffset", DEFAULT.modelMicroOffset());
        PLACEMENT_ANIMATION = client.comment("Animate newly placed ingredient models with a short bounce.")
                .define("placementAnimation", DEFAULT.placementAnimation());
        PLACEMENT_PREVIEW_ALPHA = client.comment(
                        "Opacity of the ingredient placement preview. Set to 0 to disable it.")
                .defineInRange("placementPreviewAlpha", DEFAULT.placementPreviewAlpha(), 0.0, 1.0);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void replace(Snapshot snapshot) {
        CURRENT.set(snapshot);
    }

    public static void reset() {
        CURRENT.set(DEFAULT);
    }

    static void reloadCommon() {
        CURRENT.updateAndGet(current -> new Snapshot(
                WOODEN_PLATE_CAPACITY.getAsInt(), PORCELAIN_CAPACITY.getAsInt(), SOUP_CAPACITY.getAsInt(),
                DISH_BASE_HEIGHT.getAsInt(), SOUP_BASE_HEIGHT.getAsInt(),
                WOODEN_MAX_MODEL_HEIGHT.getAsInt(), PORCELAIN_MAX_MODEL_HEIGHT.getAsInt(),
                ALLOW_HANDHELD_FEAST_EATING.getAsBoolean(), DEBUG_LOGGING.getAsBoolean(),
                current.modelMicroOffset(), current.placementAnimation(), current.placementPreviewAlpha()));
    }

    static void reloadClient() {
        CURRENT.updateAndGet(current -> new Snapshot(
                current.woodenPlateCapacity(), current.porcelainCapacity(), current.soupCapacity(),
                current.dishBaseHeight(), current.soupBaseHeight(),
                current.woodenMaxModelHeight(), current.porcelainMaxModelHeight(),
                current.allowHandheldFeastEating(), current.debugLogging(),
                MODEL_MICRO_OFFSET.getAsBoolean(), PLACEMENT_ANIMATION.getAsBoolean(),
                PLACEMENT_PREVIEW_ALPHA.getAsDouble()));
    }

    public record Snapshot(int woodenPlateCapacity, int porcelainCapacity, int soupCapacity,
                           int dishBaseHeight, int soupBaseHeight,
                           int woodenMaxModelHeight, int porcelainMaxModelHeight,
                           boolean allowHandheldFeastEating, boolean debugLogging,
                           boolean modelMicroOffset, boolean placementAnimation,
                           double placementPreviewAlpha) {
        public Snapshot withHandheldFeastEating(boolean enabled) {
            return new Snapshot(woodenPlateCapacity, porcelainCapacity, soupCapacity,
                    dishBaseHeight, soupBaseHeight, woodenMaxModelHeight, porcelainMaxModelHeight,
                    enabled, debugLogging, modelMicroOffset, placementAnimation, placementPreviewAlpha);
        }
    }

    private GeneralConfig() {
    }
}
