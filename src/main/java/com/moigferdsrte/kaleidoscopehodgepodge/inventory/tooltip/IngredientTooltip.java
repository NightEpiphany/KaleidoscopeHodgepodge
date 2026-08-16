package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record IngredientTooltip(List<ResourceLocation> ingredientIds, List<ResourceLocation> sourceDishIds)
        implements TooltipComponent {
    public IngredientTooltip {
        ingredientIds = List.copyOf(ingredientIds);
        sourceDishIds = List.copyOf(sourceDishIds);
    }
}
