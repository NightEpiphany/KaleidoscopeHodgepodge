package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public enum PackingIngredients {
    // Vanilla
    CAKE(SuitableFor.DISH, "cake", vanillaId("cake"), new Size(14, 8, 14), null, 7),
    // StackableFoodBlock
    BAMBOO_TUBE_RICE(SuitableFor.DISH, "bamboo_tube_rice", cookeryId("bamboo_tube_rice"), new Size(6, 8, 6), null),
    // FoodBiteBlock
    RED_BERRY(SuitableFor.BOTH, "red_berry", cookeryId("blaze_lamb_chop"), new Size(2, 2, 2), 1),
    ARDENT_CORE(SuitableFor.BOTH, "ardent_core", cookeryId("blaze_lamb_chop"), new Size(3, 3, 3), 2),
    BLAZE_ROD(SuitableFor.BOTH, "blaze_rod", cookeryId("blaze_lamb_chop"), new Size(2, 9, 2), 4),
    PURPLE_BERRY(SuitableFor.BOTH, "purple_berry", cookeryId("crystal_lamb_chop"), new Size(2, 2, 2), 1),
    CRYSTAL(SuitableFor.BOTH, "crystal", cookeryId("crystal_lamb_chop"), new Size(10, 8, 6), 1, false),
    BLUE_BERRY(SuitableFor.BOTH, "blue_berry", cookeryId("frost_lamb_chop"), new Size(2, 2, 2), 1),
    ICE_CUBE(SuitableFor.BOTH, "ice_cube", cookeryId("frost_lamb_chop"), new Size(3, 3, 3), 5, false),
    MUTTON(SuitableFor.DISH, "mutton", cookeryIds("blaze_lamb_chop", "crystal_lamb_chop", "frost_lamb_chop"), new Size(10, 2, 6), 1, 3),
    BRAISED_FISH(SuitableFor.BOTH, "braised_fish", cookeryId("braised_fish"), new Size(12, 3, 5), 2),
    BRAISED_PORK_RIBS(SuitableFor.BOTH, "braised_pork_ribs", cookeryId("braised_pork_ribs"), new Size(5, 4, 7), 4),
    BROWN_MUSHROOM(SuitableFor.BOTH, "brown_mushroom", cookeryId("brown_mushroom_pot_soup"), new Size(3, 6, 3), 2),
    ASPARAGUS(SuitableFor.BOTH, "asparagus", cookeryId("buddha_jumps_over_the_wall"), new Size(3, 3, 3), 1),
    FISH_BALL(SuitableFor.BOTH, "fish_ball", cookeryId("buddha_jumps_over_the_wall"), new Size(2, 2, 2), 1),
    TREPANG_MEAT(SuitableFor.BOTH, "trepang_meat", cookeryId("buddha_jumps_over_the_wall"), new Size(2, 2, 2), 2),
    CANDIED_POTATO(SuitableFor.BOTH, "candied_potato", cookeryId("candied_potato"), new Size(4, 3, 5), 5),
    CHORUS_FRIED_EGG_TINY(SuitableFor.BOTH, "chorus_fried_egg_tiny", cookeryId("chorus_fried_egg"), new Size(5, 7, 5), 1),
    CHORUS_FRIED_EGG_MEDIAN(SuitableFor.BOTH, "chorus_fried_egg_median", cookeryId("chorus_fried_egg"), new Size(5, 5, 5), 1),
    CHORUS_FRIED_EGG_LARGE(SuitableFor.BOTH, "chorus_fried_egg_large", cookeryId("chorus_fried_egg"), new Size(7, 9, 7), 1),
    COLD_ROASTED_MEAT(SuitableFor.BOTH, "cold_roasted_meat", cookeryId("cold_roasted_meat"), new Size(9, 3, 8), 2),
    SASHIMI(SuitableFor.BOTH, "sashimi", cookeryIds("cold_style_sashimi", "desert_style_sashimi", "end_style_sashimi", "nether_style_sashimi", "tundra_style_sashimi"), new Size(2, 4, 6), 4),
    SPRUCE_DECO(SuitableFor.BOTH, "spruce_deco", cookeryId("cold_style_sashimi"), new Size(6, 9, 6), 1, false),
    SNOWMAN_DECO(SuitableFor.BOTH, "snowman_deco", cookeryId("cold_style_sashimi"), new Size(4, 6, 4), 2, false),
    FLOWERS_DECO(SuitableFor.BOTH, "flowers_deco", cookeryId("tundra_style_sashimi"), new Size(14, 7, 6), 1, false),
    CACTUS_DECO(SuitableFor.BOTH, "cactus_deco", cookeryId("desert_style_sashimi"), new Size(10, 11, 5), 1, false),
    CHORUS_DECO(SuitableFor.BOTH, "chorus_deco", cookeryId("end_style_sashimi"), new Size(14, 9, 4), 1),
    FUNGUS_DECO(SuitableFor.BOTH, "fungus_deco", cookeryId("nether_style_sashimi"), new Size(14, 7, 8), 1),
    CRIMSON_FUNGUS(SuitableFor.BOTH, "crimson_fungus", cookeryId("crimson_fungus_pot_soup"), new Size(5, 4, 5), 1),
    DARK_CUISINE(SuitableFor.BOTH, "dark_cuisine", cookeryId("dark_cuisine"), new Size(4, 3, 4), 5),
    BAMBOO_DECO(SuitableFor.BOTH, "bamboo_deco", cookeryId("dongpo_pork"), new Size(2, 16, 2), 2, false),
    DONGPO_PORK(SuitableFor.BOTH, "dongpo_pork", cookeryId("dongpo_pork"), new Size(3, 6, 8), 3),
    CARROT_SLICE(SuitableFor.BOTH, "carrot_slice", cookeryId("dough_drop_soup"), new Size(4, 3, 4), 1),
    DOUGH_DROP(SuitableFor.BOTH, "dough_drop", cookeryId("dough_drop_soup"), new Size(2, 2, 2), 6),
    FONDANT_PIE(SuitableFor.BOTH, "fondant_pie", cookeryId("fondant_pie"), new Size(10, 5, 10), 1, 4),
    FONDANT_SPIDER_EYE(SuitableFor.DISH, "fondant_spider_eye", cookeryId("fondant_spider_eye"), new Size(14, 7, 13), 1, 4),
    CELERY_CHUNK(SuitableFor.BOTH, "celery_chunk", cookeryId("four_joy_meatball_soup"), new Size(2, 2, 2), 3),
    MEATBALL(SuitableFor.BOTH, "meatball", cookeryId("four_joy_meatball_soup"), new Size(4, 4, 4), 4),
    FRIED_CATERPILLAR(SuitableFor.BOTH, "fried_caterpillar", cookeryId("fried_caterpillar"), new Size(4, 3, 16), 3),
    FRIED_SPRING_ROLL(SuitableFor.BOTH, "fried_spring_roll", cookeryId("fried_spring_roll"), new Size(4, 3, 8), 3),
    FRIED_SPRING_ROLL_CHUNK(SuitableFor.BOTH, "fried_spring_roll_chunk", cookeryId("fried_spring_roll"), new Size(4, 4, 4), 1),
    FRIED_SPRING_ROLL_SAUCE_DECO(SuitableFor.BOTH, "fried_spring_roll_sauce_deco", cookeryId("fried_spring_roll"), new Size(4, 2, 4), 1, false),
    APPLE(SuitableFor.BOTH, "apple", cookeryId("fruit_platter"), new Size(4, 4, 4), 2),
    GIANT_RED_BERRY(SuitableFor.BOTH, "giant_red_berry", cookeryId("fruit_platter"), new Size(3, 3, 3), 3),
    GIANT_GLOW_BERRY(SuitableFor.BOTH, "giant_glow_berry", cookeryId("fruit_platter"), new Size(3, 3, 3), 4),
    GOLDEN_APPLE(SuitableFor.BOTH, "golden_apple", cookeryId("golden_salad"), new Size(4, 4, 4), 3),
    GLISTERING_MELON(SuitableFor.BOTH, "glistering_melon", cookeryId("golden_salad"), new Size(7, 6, 2), 3),
    NUMBING_SPICY_CHICKEN(SuitableFor.BOTH, "numbing_spicy_chicken", cookeryId("numbing_spicy_chicken"), new Size(2, 3, 5), 6),
    GREEN_PEPPER_CHUNK(SuitableFor.BOTH, "green_pepper_chunk", cookeryId("numbing_spicy_chicken"), new Size(2, 2, 2), 1),
    RED_PEPPER_CHUNK(SuitableFor.BOTH, "red_pepper_chunk", cookeryId("numbing_spicy_chicken"), new Size(2, 2, 2), 1),
    ;


    private final SuitableFor suitableFor;
    private final Identifier id;
    private final List<Identifier> srcFoodIds;
    private final Size size;
    private final String resourceLoc;
    private final boolean hasNutrition;
    private final int modelStack;

    @Nullable
    private final Integer countPerDish;
    private final Map<Identifier, Integer> countPerDishOverrides;

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size, @Nullable Integer countPerDish, int modelStack) {
        this(suitableFor, id, List.of(srcFoodId), size, countPerDish, true, false, modelStack);
    }

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size, @Nullable Integer countPerDish, boolean hasNutrition, int modelStack) {
        this(suitableFor, id, List.of(srcFoodId), size, countPerDish, hasNutrition, false, modelStack);
    }

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size, @Nullable Integer countPerDish) {
        this(suitableFor, id, List.of(srcFoodId), size, countPerDish, true, false, 1);
    }

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size, @Nullable Integer countPerDish, boolean hasNutrition) {
        this(suitableFor, id, List.of(srcFoodId), size, countPerDish, hasNutrition, false, 1);
    }

    PackingIngredients(SuitableFor suitableFor, String id, List<Identifier> srcFoodIds, Size size,
                       @Nullable Integer countPerDish, int modelStack) {
        this(suitableFor, id, srcFoodIds, size, countPerDish, true, true, modelStack);
    }

    PackingIngredients(SuitableFor suitableFor, String id, List<Identifier> srcFoodIds, Size size,
                       @Nullable Integer countPerDish) {
        this(suitableFor, id, srcFoodIds, size, countPerDish, true, true, 1);
    }

    PackingIngredients(SuitableFor suitableFor, String id, List<Identifier> srcFoodIds, Size size,
                       @Nullable Integer countPerDish, boolean hasNutrition) {
        this(suitableFor, id, srcFoodIds, size, countPerDish, hasNutrition, true, 1);
    }

    PackingIngredients(SuitableFor suitableFor, String id, List<Identifier> srcFoodIds, Size size,
                       @Nullable Integer countPerDish, StoreUnit... countPerDishOverrides) {
        this(suitableFor, id, srcFoodIds, size, countPerDish, true, true, 1, countPerDishOverrides);
    }

    PackingIngredients(SuitableFor suitableFor, String id, List<Identifier> srcFoodIds, Size size,
                       @Nullable Integer countPerDish, boolean hasNutrition,
                       StoreUnit... countPerDishOverrides) {
        this(suitableFor, id, srcFoodIds, size, countPerDish, hasNutrition, true, 1, countPerDishOverrides);
    }

    PackingIngredients(SuitableFor suitableFor, String id, List<Identifier> srcFoodIds, Size size,
                       @Nullable Integer countPerDish, boolean hasNutrition, boolean commonModel, int modelStack,
                       StoreUnit... countPerDishOverrides) {
        if (srcFoodIds.isEmpty()) throw new IllegalArgumentException("An ingredient needs at least one source food");
        if (srcFoodIds.stream().distinct().count() != srcFoodIds.size()) {
            throw new IllegalArgumentException("Ingredient source foods must be unique: " + id);
        }
        if (modelStack < 1) throw new IllegalArgumentException("Model stack must be positive: " + id);
        this.suitableFor = suitableFor;
        this.id = KaleidoscopeHodgepodge.id(id);
        this.srcFoodIds = List.copyOf(srcFoodIds);
        this.size = size;
        this.modelStack = modelStack;
        this.countPerDish = countPerDish;
        this.countPerDishOverrides = createCountOverrides(id, this.srcFoodIds, countPerDishOverrides);
        this.resourceLoc = "packing_ingredients/"
                + (commonModel ? "common" : this.srcFoodIds.getFirst().getPath()) + "/" + id;
        this.hasNutrition = hasNutrition;
    }

    public SuitableFor suitableFor() {
        return suitableFor;
    }

    public Identifier getId() {
        return id;
    }

    public List<Identifier> getSrcFoodIds() {
        return srcFoodIds;
    }

    public Size getSize() {
        return size;
    }

    public String getResourceLoc() {
        return resourceLoc;
    }

    public boolean hasNutrition() {
        return hasNutrition;
    }

    public int getModelStack() {
        return modelStack;
    }

    public @Nullable Integer getCountPerDish() {
        return countPerDish;
    }

    public @Nullable Integer getCountPerDish(Identifier srcFoodId) {
        return countPerDishOverrides.getOrDefault(srcFoodId, countPerDish);
    }

    private static Map<Identifier, Integer> createCountOverrides(
            String ingredientId, List<Identifier> srcFoodIds, StoreUnit[] overrides) {
        Map<Identifier, Integer> result = new HashMap<>();
        for (StoreUnit override : overrides) {
            if (!srcFoodIds.contains(override.str())) {
                throw new IllegalArgumentException("Count override source is not registered for " + ingredientId
                        + ": " + override.str());
            }
            if (override.counts() < 0) {
                throw new IllegalArgumentException("Count override cannot be negative: " + ingredientId);
            }
            if (result.putIfAbsent(override.str(), override.counts()) != null) {
                throw new IllegalArgumentException("Duplicate count override for " + ingredientId
                        + ": " + override.str());
            }
        }
        return Map.copyOf(result);
    }

    private static Identifier vanillaId(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static List<Identifier> vanillaIds(String... paths) {
        return Arrays.stream(paths).map(PackingIngredients::vanillaId).toList();
    }

    private static Identifier cookeryId(String path) {
        return Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, path);
    }

    private static List<Identifier> cookeryIds(String... paths) {
        return Arrays.stream(paths).map(PackingIngredients::cookeryId).toList();
    }

    /**
     * This record class is used to describe the exact size of the model.
     * Renderer based on NONE ItemDisplayContext.
     * @see ItemDisplayContext
     * @param x West to East Axis
     * @param y Down to Up Axis
     * @param z North to South Axis
     */
    public record Size(int x, int y, int z){}
    public record StoreUnit(Identifier str, int counts) {}
    public enum SuitableFor {
        BOTH,
        DISH,
        SOUP
    }
}
