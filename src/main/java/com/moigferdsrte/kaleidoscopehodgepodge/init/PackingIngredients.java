package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.resources.Identifier;

public enum PackingIngredients {
    BAMBOO_TUBE_RICE(SuitableFor.DISH, "bamboo_tube_rice", new Size(6, 8, 6))
    ;


    private final SuitableFor suitableFor;
    private final Identifier id;
    private final Size size;

    PackingIngredients(SuitableFor suitableFor, String id, Size size) {
        this.suitableFor = suitableFor;
        this.id = KaleidoscopeHodgepodge.id(id);
        this.size = size;
    }

    public SuitableFor suitableFor() {
        return suitableFor;
    }

    public Identifier getId() {
        return id;
    }

    public Size getSize() {
        return size;
    }

    public record Size(int x, int y, int z){}
    public enum SuitableFor {
        BOTH,
        DISH,
        SOUP
    }
}
