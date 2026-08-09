package com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodBiteBlock.class)
public interface FoodBiteBlockAccessor {
    @Accessor("foodProperties")
    FoodProperties kaleidoscopeHodgepodge$getFoodProperties();

    @Accessor("consumable")
    Consumable kaleidoscopeHodgepodge$getConsumable();
}
