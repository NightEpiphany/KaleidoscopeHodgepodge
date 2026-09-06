package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/*杂烩提示信息*/
public record FeastIngredientsTooltip(List<Identifier> ingredientIds) implements TooltipComponent {
    public FeastIngredientsTooltip {
        ingredientIds = List.copyOf(ingredientIds);
    }
}
