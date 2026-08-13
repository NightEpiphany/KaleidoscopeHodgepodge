package com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PotBlockEntity.class)
public interface PotBlockEntityAccessor {
    @Accessor("status")
    void kaleidoscopeHodgepodge$setStatus(int status);

    @Accessor("result")
    void kaleidoscopeHodgepodge$setResult(ItemStack result);
}
