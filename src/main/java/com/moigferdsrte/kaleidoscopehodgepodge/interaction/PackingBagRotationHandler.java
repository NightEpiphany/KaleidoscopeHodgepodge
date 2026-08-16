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
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class PackingBagRotationHandler {
    public static void init() {
        NeoForge.EVENT_BUS.addListener(PackingBagRotationHandler::onLeftClickBlock);
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (handle(event.getEntity(), event.getLevel(), event.getHand(), event.getPos()).consumesAction()) {
            event.setCanceled(true);
        }
    }

    public static InteractionResult handle(Player player, Level level, InteractionHand hand, BlockPos pos) {
        if (player.isSpectator()
                || !(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(KHItems.WRAPPING_BAG.get())
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
