package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record IngredientTooltip(Identifier ingredientId) implements TooltipComponent {}
