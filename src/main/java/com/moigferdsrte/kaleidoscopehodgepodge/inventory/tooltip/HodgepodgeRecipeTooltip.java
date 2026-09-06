package com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record HodgepodgeRecipeTooltip(ItemStack preview, List<PlacedIngredient> placements, UUID owner,
                                      Optional<GameProfile> ownerProfile, Optional<Component> dishName) implements TooltipComponent {
    public HodgepodgeRecipeTooltip(List<PlacedIngredient> placements, UUID owner) {
        this(ItemStack.EMPTY, placements, owner);
    }

    public HodgepodgeRecipeTooltip(ItemStack preview, List<PlacedIngredient> placements, UUID owner) {
        this(preview, placements, owner, Optional.empty());
    }

    public HodgepodgeRecipeTooltip(ItemStack preview, List<PlacedIngredient> placements, UUID owner,
                                   Optional<GameProfile> ownerProfile) {
        this(preview, placements, owner, ownerProfile, Optional.empty());
    }

    public HodgepodgeRecipeTooltip {
        preview = preview == null ? ItemStack.EMPTY : preview.copy();
        placements = List.copyOf(placements);
        dishName = dishName.filter(value -> !value.getString().isBlank()).map(Component::copy);
    }
}
