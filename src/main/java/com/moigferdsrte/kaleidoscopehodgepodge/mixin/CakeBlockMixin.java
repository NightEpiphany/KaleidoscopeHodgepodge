package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$useWrappingBag(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        InteractionHand hand = wrappingBagHand(player);
        if (hand == null) return;

        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = stack.getItem().useOn(new UseOnContext(level, player, hand, stack, hit));
        // 手持纸袋时必须截断蛋糕的原版进食路径。
        cir.setReturnValue(result == InteractionResult.PASS ? InteractionResult.FAIL : result);
    }

    @Unique
    private static InteractionHand wrappingBagHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof WrappingBagItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof WrappingBagItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
