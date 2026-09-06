package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.UUID;
public record HodgepodgeRecipeTooltip(ItemStack preview, List<PlacedIngredient> placements, UUID owner) implements TooltipComponent {
 public HodgepodgeRecipeTooltip(List<PlacedIngredient> placements, UUID owner) {
  this(ItemStack.EMPTY, placements, owner);
 }
 public HodgepodgeRecipeTooltip {
  preview = preview == null ? ItemStack.EMPTY : preview.copy();
  placements = List.copyOf(placements);
 }
}
