package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

public enum PackingIngredients {
    // StackableFoodBlock
    BAMBOO_TUBE_RICE(SuitableFor.DISH, "bamboo_tube_rice", cookeryId("bamboo_tube_rice"), new Size(6, 8, 6), null),
    // FoodBiteBlock
    RED_BERRY(SuitableFor.BOTH, "red_berry", cookeryId("blaze_lamb_chop"), new Size(2, 2, 2), 1),
    MUTTON(SuitableFor.DISH, "mutton", cookeryId("blaze_lamb_chop"), new Size(10, 2, 6), 1),
    ARDENT_CORE(SuitableFor.BOTH, "ardent_core", cookeryId("blaze_lamb_chop"), new Size(3, 3, 3), 2),
    BLAZE_ROD(SuitableFor.BOTH, "blaze_rod", cookeryId("blaze_lamb_chop"), new Size(2, 9, 2), 4),
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
    SASHIMI(SuitableFor.BOTH, "sashimi", cookeryId("cold_style_sashimi"), new Size(2, 4, 6), 4),
    SPRUCE_DECO(SuitableFor.BOTH, "spruce_deco", cookeryId("cold_style_sashimi"), new Size(6, 9, 6), 1, false),
    SNOWMAN_DECO(SuitableFor.BOTH, "snowman_deco", cookeryId("cold_style_sashimi"), new Size(4, 6, 4), 2, false),
    CRIMSON_FUNGUS(SuitableFor.BOTH, "crimson_fungus", cookeryId("crimson_fungus_pot_soup"), new Size(5, 4, 5), 1),
    DARK_CUISINE(SuitableFor.BOTH, "dark_cuisine", cookeryId("dark_cuisine"), new Size(4, 3, 4), 5),
    ;


    private final SuitableFor suitableFor;
    private final Identifier id;
    private final Identifier srcFoodId;
    private final Size size;
    private final String resourceLoc;

    private final boolean hasNutrition;

    @Nullable
    private final Integer countPerDish;

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size, @Nullable Integer countPerDish) {
        this(suitableFor, id, srcFoodId, size, countPerDish, true);
    }

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size, @Nullable Integer countPerDish, boolean hasNutrition) {
        this.suitableFor = suitableFor;
        this.id = KaleidoscopeHodgepodge.id(id);
        this.srcFoodId = srcFoodId;
        this.size = size;
        this.countPerDish = countPerDish;
        this.resourceLoc = "packing_ingredients/" + srcFoodId.getPath() + "/" + id;
        this.hasNutrition = hasNutrition;
    }

    public SuitableFor suitableFor() {
        return suitableFor;
    }

    public Identifier getId() {
        return id;
    }

    public Identifier getSrcFoodId() {
        return srcFoodId;
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


    public @Nullable Integer getCountPerDish() {
        return countPerDish;
    }

    private static Identifier cookeryId(String path) {
        return Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, path);
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
    public enum SuitableFor {
        BOTH,
        DISH,
        SOUP
    }
}
