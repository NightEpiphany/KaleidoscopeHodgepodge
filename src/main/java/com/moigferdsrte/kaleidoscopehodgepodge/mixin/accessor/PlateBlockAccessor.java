package com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlateBlock.class)
public interface PlateBlockAccessor {

    @Accessor("servings")
    IntegerProperty kaleidoscopeHodgepodge$getServings();

    @Accessor("maxCount")
    int kaleidoscopeHodgepodge$getMaxCount();
}
