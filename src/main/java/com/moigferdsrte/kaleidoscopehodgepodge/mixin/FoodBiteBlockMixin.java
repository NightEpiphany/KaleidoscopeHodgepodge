package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FoodBiteStructureService;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(FoodBiteBlock.class)
public class FoodBiteBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$useWrappingBag(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        InteractionHand hand = wrappingBagHand(player);
        if (hand == null) return;

        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = stack.getItem().useOn(
                new UseOnContext(level, player, hand, stack, hit));
        // 放置模式等返回 PASS 时也必须截断，纸袋不能触发菜品的 eat。
        cir.setReturnValue(result == InteractionResult.PASS ? InteractionResult.FAIL : result);
    }

    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$suppressPackedStructureDrops(
            @NonNull BlockState state, LootParams.@NonNull Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (FoodBiteStructureService.isSuppressingDrops()) cir.setReturnValue(List.of());
    }

    @Unique
    private static InteractionHand wrappingBagHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof WrappingBagItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof WrappingBagItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
