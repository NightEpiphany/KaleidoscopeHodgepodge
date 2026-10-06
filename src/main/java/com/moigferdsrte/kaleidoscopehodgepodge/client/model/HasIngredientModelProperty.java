package com.moigferdsrte.kaleidoscopehodgepodge.client.model;

import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Item-model condition used to keep empty containers on the generated 2D model. */
@Environment(EnvType.CLIENT)
public record HasIngredientModelProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<HasIngredientModelProperty> MAP_CODEC = MapCodec.unit(new HasIngredientModelProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity,
                       int seed, @NonNull ItemDisplayContext displayContext) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        return feast != null && feast.ingredients().stream()
                .anyMatch(ingredient -> IngredientModelService.hasModel(ingredient.id()));
    }

    @Override
    public @NonNull MapCodec<HasIngredientModelProperty> type() {
        return MAP_CODEC;
    }
}
