package com.moigferdsrte.kaleidoscopehodgepodge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.concurrent.atomic.AtomicReference;

/*配置信息*/
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
    private static final ModConfigSpec.BooleanValue ALLOW_HANDHELD_DISH_EATING;
    private static final ModConfigSpec.BooleanValue ALLOW_HANDHELD_SOUP_EATING;
    private static final ModConfigSpec.BooleanValue INGREDIENT_MODEL_COLLISION;
    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    private static final ModConfigSpec.BooleanValue MODEL_MICRO_OFFSET;
    private static final ModConfigSpec.BooleanValue PLACEMENT_ANIMATION;
    private static final ModConfigSpec.BooleanValue WRAPPING_BAG_INGREDIENT_PREVIEW;
    private static final ModConfigSpec.BooleanValue LUNCH_BOX_INGREDIENT_PREVIEW;
    private static final ModConfigSpec.DoubleValue PLACEMENT_PREVIEW_ALPHA;

    private static final Snapshot DEFAULT = new Snapshot(
            20, 40, 40, 2, 4, 16, 32,
            false, true, true, false, true, true, 0.4D, true, true);
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
        ALLOW_HANDHELD_DISH_EATING = common.comment(
                        "Allow a filled custom plate item to be eaten directly from the player's hand.")
                .define("allowHandheldDishEating", DEFAULT.allowHandheldDishEating());
        ALLOW_HANDHELD_SOUP_EATING = common.comment(
                        "Allow a filled custom soup bowl item to be consumed directly from the player's hand.")
                .define("allowHandheldSoupEating", DEFAULT.allowHandheldSoupEating());
        INGREDIENT_MODEL_COLLISION = common.comment(
                        "Make ingredient models collide using four 8px by 8px height-map regions per container block.")
                .define("ingredientModelCollision", DEFAULT.ingredientModelCollision());
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
        WRAPPING_BAG_INGREDIENT_PREVIEW = client.comment(
                        "Show the first ingredient model in the lower-right quarter of filled wrapping bags in GUI slots.")
                .define("wrappingBagIngredientPreview", DEFAULT.wrappingBagIngredientPreview());
        LUNCH_BOX_INGREDIENT_PREVIEW = client.comment(
                        "Show the selected ingredient model in the lower-right quarter of lunch boxes in GUI slots.")
                .define("lunchBoxIngredientPreview", DEFAULT.lunchBoxIngredientPreview());
        PLACEMENT_PREVIEW_ALPHA = client.comment(
                        "Opacity of the ingredient placement preview. Set to 0 to disable it.")
                .defineInRange("placementPreviewAlpha", DEFAULT.placementPreviewAlpha(), 0.0D, 1.0D);
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
                ALLOW_HANDHELD_DISH_EATING.getAsBoolean(), ALLOW_HANDHELD_SOUP_EATING.getAsBoolean(),
                INGREDIENT_MODEL_COLLISION.getAsBoolean(),
                DEBUG_LOGGING.getAsBoolean(),
                current.modelMicroOffset(), current.placementAnimation(), current.placementPreviewAlpha(),
                current.wrappingBagIngredientPreview(), current.lunchBoxIngredientPreview()));
    }

    static void reloadClient() {
        CURRENT.updateAndGet(current -> new Snapshot(
                current.woodenPlateCapacity(), current.porcelainCapacity(), current.soupCapacity(),
                current.dishBaseHeight(), current.soupBaseHeight(),
                current.woodenMaxModelHeight(), current.porcelainMaxModelHeight(),
                current.allowHandheldDishEating(), current.allowHandheldSoupEating(),
                current.ingredientModelCollision(), current.debugLogging(),
                MODEL_MICRO_OFFSET.getAsBoolean(), PLACEMENT_ANIMATION.getAsBoolean(),
                PLACEMENT_PREVIEW_ALPHA.getAsDouble(), WRAPPING_BAG_INGREDIENT_PREVIEW.getAsBoolean(),
                LUNCH_BOX_INGREDIENT_PREVIEW.getAsBoolean()));
    }

    public record Snapshot(int woodenPlateCapacity, int porcelainCapacity, int soupCapacity,
                           int dishBaseHeight, int soupBaseHeight,
                           int woodenMaxModelHeight, int porcelainMaxModelHeight,
                           boolean allowHandheldDishEating, boolean allowHandheldSoupEating,
                           boolean ingredientModelCollision,
                           boolean debugLogging,
                           boolean modelMicroOffset, boolean placementAnimation,
                           double placementPreviewAlpha, boolean wrappingBagIngredientPreview,
                           boolean lunchBoxIngredientPreview) {
        public Snapshot(int woodenPlateCapacity, int porcelainCapacity, int soupCapacity,
                        int dishBaseHeight, int soupBaseHeight,
                        int woodenMaxModelHeight, int porcelainMaxModelHeight,
                        boolean allowHandheldDishEating, boolean allowHandheldSoupEating,
                        boolean debugLogging,
                        boolean modelMicroOffset, boolean placementAnimation,
                        double placementPreviewAlpha, boolean wrappingBagIngredientPreview) {
            this(woodenPlateCapacity, porcelainCapacity, soupCapacity,
                    dishBaseHeight, soupBaseHeight, woodenMaxModelHeight, porcelainMaxModelHeight,
                    allowHandheldDishEating, allowHandheldSoupEating, true, debugLogging,
                    modelMicroOffset, placementAnimation, placementPreviewAlpha,
                    wrappingBagIngredientPreview, true);
        }

        public Snapshot(int woodenPlateCapacity, int porcelainCapacity, int soupCapacity,
                        int dishBaseHeight, int soupBaseHeight,
                        int woodenMaxModelHeight, int porcelainMaxModelHeight,
                        boolean allowHandheldDishEating, boolean allowHandheldSoupEating,
                        boolean ingredientModelCollision,
                        boolean debugLogging,
                        boolean modelMicroOffset, boolean placementAnimation,
                        double placementPreviewAlpha, boolean wrappingBagIngredientPreview) {
            this(woodenPlateCapacity, porcelainCapacity, soupCapacity,
                    dishBaseHeight, soupBaseHeight, woodenMaxModelHeight, porcelainMaxModelHeight,
                    allowHandheldDishEating, allowHandheldSoupEating, ingredientModelCollision,
                    debugLogging,
                    modelMicroOffset, placementAnimation, placementPreviewAlpha,
                    wrappingBagIngredientPreview, true);
        }

        public Snapshot withHandheldFeastEating(boolean enabled) {
            return new Snapshot(woodenPlateCapacity, porcelainCapacity, soupCapacity,
                    dishBaseHeight, soupBaseHeight, woodenMaxModelHeight, porcelainMaxModelHeight,
                    enabled, enabled, ingredientModelCollision, debugLogging,
                    modelMicroOffset, placementAnimation, placementPreviewAlpha,
                    wrappingBagIngredientPreview, lunchBoxIngredientPreview);
        }

        public Snapshot withIngredientModelCollision(boolean enabled) {
            return new Snapshot(woodenPlateCapacity, porcelainCapacity, soupCapacity,
                    dishBaseHeight, soupBaseHeight, woodenMaxModelHeight, porcelainMaxModelHeight,
                    allowHandheldDishEating, allowHandheldSoupEating, enabled, debugLogging,
                    modelMicroOffset, placementAnimation, placementPreviewAlpha,
                    wrappingBagIngredientPreview, lunchBoxIngredientPreview);
        }

        /** 兼容旧测试及扩展代码；新配置应分别使用盘子和汤碗开关。 */
        @Deprecated
        public boolean allowHandheldFeastEating() {
            return allowHandheldDishEating || allowHandheldSoupEating;
        }
    }

    private GeneralConfig() {
    }
}
