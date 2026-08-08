package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

public enum PackingIngredients {
    // StackableFoodBlock
    BAMBOO_TUBE_RICE(SuitableFor.DISH, "bamboo_tube_rice", Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "bamboo_tube_rice"), new Size(6, 8, 6)),
    // FoodBiteBlock
    RED_BERRY(SuitableFor.BOTH, "red_berry", FoodBiteRegistry.BLAZE_LAMB_CHOP, new Size(2, 2, 2)),
    MUTTON(SuitableFor.DISH, "mutton", FoodBiteRegistry.BLAZE_LAMB_CHOP, new Size(10, 2, 6)),
    ARDENT_CORE(SuitableFor.BOTH, "ardent_core", FoodBiteRegistry.BLAZE_LAMB_CHOP, new Size(3, 3, 3)),
    BLAZE_ROD(SuitableFor.BOTH, "blaze_rod", FoodBiteRegistry.BLAZE_LAMB_CHOP, new Size(2, 8, 2)),
    ;


    private final SuitableFor suitableFor;
    private final Identifier id;
    private final Identifier srcFoodId;
    private final Size size;
    private final String resourceLoc;

    PackingIngredients(SuitableFor suitableFor, String id, Identifier srcFoodId, Size size) {
        this.suitableFor = suitableFor;
        this.id = KaleidoscopeHodgepodge.id(id);
        this.srcFoodId = srcFoodId;
        this.size = size;
        this.resourceLoc = "packing_ingredients/" + srcFoodId.getPath() + "/" + id;
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
