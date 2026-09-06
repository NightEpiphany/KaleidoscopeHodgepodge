package com.moigferdsrte.kaleidoscopehodgepodge.interaction;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.network.RotatePackingBagPayload;
import com.moigferdsrte.kaleidoscopehodgepodge.network.BulkPlacePackingBagPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class PackingBagRotationHandler {
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(BulkPlacePackingBagPayload.TYPE,
                BulkPlacePackingBagPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BulkPlacePackingBagPayload.TYPE,
                (payload, context) -> bulkPlace(context.player(), context.player().level(),
                        payload.pos(), payload.location(), payload.direction(), payload.inside(), payload.hand()));
        PayloadTypeRegistry.serverboundPlay().register(RotatePackingBagPayload.TYPE,
                RotatePackingBagPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RotatePackingBagPayload.TYPE,
                (payload, context) -> rotate(context.player(), context.player().level(), payload.pos(), payload.hand()));
    }

    public static boolean canRotate(Player player, Level level, BlockPos pos) {
        return canRotate(player, level, pos, InteractionHand.MAIN_HAND);
    }

    public static boolean canRotate(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (player.isSpectator()
                || !(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(KHItems.LUNCH_BOX)) {
            BaggedIngredient ingredient = LunchBoxService.selectedIngredient(stack);
            return LunchBoxService.getMode(stack) == PackingBagMode.PLACEMENT
                    && ingredient != null
                    && suitable(ingredient, feast.kind());
        }
        if (!stack.is(KHItems.WRAPPING_BAG)
                || PackingBagService.getMode(stack) != PackingBagMode.PLACEMENT) {
            return false;
        }
        BaggedIngredient current = PackingBagService.get(stack).first().orElse(null);
        if (current == null) return false;
        PackingIngredients ingredient = PackingIngredientRegistry.byId(current.id()).orElse(null);
        return ingredient != null && isSuitable(ingredient, feast.kind());
    }

    public static boolean canBulkPlace(Player player, Level level, BlockPos pos, InteractionHand hand) {
        return !player.isSpectator()
                && level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity
                && player.getItemInHand(hand).is(KHItems.WRAPPING_BAG)
                && PackingBagService.getMode(player.getItemInHand(hand)) == PackingBagMode.PLACEMENT
                && !PackingBagService.get(player.getItemInHand(hand)).isEmpty();
    }

    public static int bulkPlace(Player player, Level level, BlockPos pos, Vec3 location,
                                net.minecraft.core.Direction direction, boolean inside,
                                InteractionHand hand) {
        if (level.isClientSide()
                || player.isSpectator()
                || !player.isWithinBlockInteractionRange(pos, 1.0)
                || !level.mayInteract(player, pos)
                || !canBulkPlace(player, level, pos, hand)) {
            return 0;
        }

        BlockHitResult hit = new BlockHitResult(location, direction, pos, inside);
        ItemStack stack = player.getItemInHand(hand);
        int placed = 0;
        while (placed < PackingBagContents.MAX_INGREDIENTS && !PackingBagService.get(stack).isEmpty()) {
            InteractionResult result = level.getBlockState(pos).useItemOn(stack, level, player, hand, hit);
            if (result != InteractionResult.SUCCESS) break;
            placed++;
        }
        return placed;
    }

    public static boolean rotate(Player player, Level level, BlockPos pos) {
        return rotate(player, level, pos, InteractionHand.MAIN_HAND);
    }

    public static boolean rotate(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (level.isClientSide()
                || !player.isWithinBlockInteractionRange(pos, 1.0)
                || !level.mayInteract(player, pos)
                || !canRotate(player, level, pos, hand)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(KHItems.LUNCH_BOX)) {
            LunchBoxService.rotateSelected(stack);
        } else {
            PackingBagContents contents = PackingBagService.get(stack);
            PackingBagService.replaceHeldBag(stack, player, contents.rotateFirstClockwise());
        }
        return true;
    }

    private static boolean suitable(BaggedIngredient ingredient, CustomFeastData.ContainerKind kind) {
        PackingIngredients value = PackingIngredientRegistry.byId(ingredient.id()).orElse(null);
        return value != null && isSuitable(value, kind);
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
