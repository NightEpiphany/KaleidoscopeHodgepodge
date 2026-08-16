package com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StackableFoodBlock.class)
public interface StackableFoodBlockAccessor {
    @Accessor("countProperty")
    IntegerProperty kaleidoscopeHodgepodge$getCountProperty();

    @Accessor("maxCount")
    int kaleidoscopeHodgepodge$getMaxCount();
}
