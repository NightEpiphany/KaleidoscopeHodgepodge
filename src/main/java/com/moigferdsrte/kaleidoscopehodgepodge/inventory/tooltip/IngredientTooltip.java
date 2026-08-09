package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record IngredientTooltip(List<Identifier> ingredientIds, Identifier sourceDishId) implements TooltipComponent {
    public IngredientTooltip {
        ingredientIds = List.copyOf(ingredientIds);
    }
}
