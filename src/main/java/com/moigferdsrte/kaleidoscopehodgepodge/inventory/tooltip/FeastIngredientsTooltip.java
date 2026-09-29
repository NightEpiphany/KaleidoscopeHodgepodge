package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/*杂烩提示信息*/
public record FeastIngredientsTooltip(List<ResourceLocation> ingredientIds) implements TooltipComponent {
    public FeastIngredientsTooltip {
        ingredientIds = List.copyOf(ingredientIds);
    }
}
