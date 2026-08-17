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
import com.moigferdsrte.kaleidoscopehodgepodge.network.RotatePackingBagPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PackingBagRotationHandler {
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(RotatePackingBagPayload.TYPE,
                RotatePackingBagPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RotatePackingBagPayload.TYPE,
                (payload, context) -> rotate(context.player(), context.player().level(), payload.pos()));
    }

    public static boolean canRotate(Player player, Level level, BlockPos pos) {
        if (player.isSpectator()
                || !(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(KHItems.WRAPPING_BAG)
                || PackingBagService.getMode(stack) != PackingBagMode.PLACEMENT) {
            return false;
        }
        BaggedIngredient current = PackingBagService.get(stack).first().orElse(null);
        if (current == null) return false;
        PackingIngredients ingredient = PackingIngredientRegistry.byId(current.id()).orElse(null);
        return ingredient != null && isSuitable(ingredient, feast.kind());
    }

    public static boolean rotate(Player player, Level level, BlockPos pos) {
        if (level.isClientSide()
                || !player.isWithinBlockInteractionRange(pos, 1.0)
                || !level.mayInteract(player, pos)
                || !canRotate(player, level, pos)) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        PackingBagContents contents = PackingBagService.get(stack);
        PackingBagService.replaceHeldBag(stack, player, contents.rotateFirstClockwise());
        return true;
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
