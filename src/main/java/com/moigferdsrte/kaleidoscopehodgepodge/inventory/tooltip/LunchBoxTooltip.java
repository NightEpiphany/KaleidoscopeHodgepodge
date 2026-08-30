package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Optional;

/** Tooltip payload for the lunch box preview and total unit count. */
public record LunchBoxTooltip(Optional<Identifier> selectedIngredientId, int totalCount, int occupiedSlots)
        implements TooltipComponent {
    public LunchBoxTooltip {
        selectedIngredientId = selectedIngredientId == null ? Optional.empty() : selectedIngredientId;
        totalCount = Math.max(0, totalCount);
        occupiedSlots = Math.clamp(occupiedSlots, 0, 15);
    }
}
