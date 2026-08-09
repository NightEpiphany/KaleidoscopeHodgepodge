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

public final class PackingBagRotationHandler {
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

            // 堆叠纸袋只在服务端拆分，避免客户端生成短暂的幽灵掉落物。
            if (!level.isClientSide() || stack.getCount() == 1) {
                PackingBagService.replaceHeldBag(stack, player, contents.rotateFirstClockwise());
            }
            return InteractionResult.SUCCESS;
        });
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
