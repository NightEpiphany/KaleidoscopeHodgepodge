package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** 午餐盒的物品栏提示信息 */
public record LunchBoxTooltip(@Nullable Optional<ResourceLocation> selectedIngredientId, int totalCount, int occupiedSlots)
        implements TooltipComponent {
    public LunchBoxTooltip {
        selectedIngredientId = selectedIngredientId == null || selectedIngredientId.isEmpty() ? Optional.empty() : selectedIngredientId;
        totalCount = Math.max(0, totalCount);
        occupiedSlots = Math.clamp(occupiedSlots, 0, 15);
    }
}
