package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record FeastIngredientsTooltip(List<Identifier> ingredientIds) implements TooltipComponent {
    public FeastIngredientsTooltip {
        ingredientIds = List.copyOf(ingredientIds);
    }
}
