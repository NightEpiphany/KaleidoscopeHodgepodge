package com.moigferdsrte.kaleidoscopehodgepodge.interaction;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PackingBagRotationHandler {
    /** 客户端一次按键只允许消费一次旋转回调。 */
    private static final AtomicBoolean CLIENT_ATTACK_CONSUMED = new AtomicBoolean();

    public static void init() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.isSpectator()
                    || !(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(KHItems.WRAPPING_BAG)
                    || PackingBagService.getMode(stack) != PackingBagMode.PLACEMENT) {
                return InteractionResult.PASS;
            }
            PackingBagContents contents = PackingBagService.get(stack);
            BaggedIngredient current = contents.first().orElse(null);
            if (current == null) return InteractionResult.PASS;
            PackingIngredients ingredient = PackingIngredientRegistry.byId(current.id()).orElse(null);
            if (ingredient == null || !isSuitable(ingredient, feast.kind())) return InteractionResult.PASS;

            // continueDestroyBlock 在创造模式下会随每个 tick 重复触发回调。
            // 客户端只消费首次有效攻击，释放攻击键后由 clientTick 解锁。
            if (level.isClientSide()
                    && !CLIENT_ATTACK_CONSUMED.compareAndSet(false, true)) {
                return InteractionResult.FAIL;
            }

            // 服务端只处理一次权威旋转，避免同一次点击在客户端和服务端各累加 90 度。
            if (!level.isClientSide()) {
                PackingBagService.replaceHeldBag(stack, player, contents.rotateFirstClockwise());
            }
            return InteractionResult.SUCCESS;
        });
    }

    public static void clientTick(boolean attackKeyDown) {
        if (!attackKeyDown) {
            CLIENT_ATTACK_CONSUMED.set(false);
        }
    }

    private static boolean isSuitable(PackingIngredients ingredient, CustomFeastData.ContainerKind kind) {
        return ingredient.suitableFor() == PackingIngredients.SuitableFor.BOTH
                || kind == CustomFeastData.ContainerKind.DISH
                && ingredient.suitableFor() == PackingIngredients.SuitableFor.DISH
                || kind == CustomFeastData.ContainerKind.SOUP
                && ingredient.suitableFor() == PackingIngredients.SuitableFor.SOUP;
    }

    private PackingBagRotationHandler() {
    }
}
