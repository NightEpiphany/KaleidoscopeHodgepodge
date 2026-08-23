package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlateBlock.class)
public class PlateBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$use(ItemStack itemInHand, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        // PlateBlock 原版只处理主手；在入口截断可避免纸袋触发取餐逻辑。
        if (hand != InteractionHand.MAIN_HAND || !(itemInHand.getItem() instanceof WrappingBagItem)) return;

        InteractionResult result = itemInHand.getItem().useOn(
                new UseOnContext(level, player, hand, itemInHand, hitResult));
        // 放置模式等返回 PASS 时也必须截断，纸袋不能触发菜品的 eat。
        cir.setReturnValue(result == InteractionResult.PASS ? ItemInteractionResult.FAIL : ItemInteractionResult.SUCCESS);
    }
}