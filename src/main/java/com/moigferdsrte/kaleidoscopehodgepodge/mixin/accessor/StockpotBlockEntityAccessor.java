package com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StockpotBlockEntity.class)
public interface StockpotBlockEntityAccessor {
    @Accessor("status")
    void kaleidoscopeHodgepodge$setStatus(int status);

    @Accessor("result")
    void kaleidoscopeHodgepodge$setResult(ItemStack result);

    @Accessor("takeoutCount")
    void kaleidoscopeHodgepodge$setTakeoutCount(int count);
}
