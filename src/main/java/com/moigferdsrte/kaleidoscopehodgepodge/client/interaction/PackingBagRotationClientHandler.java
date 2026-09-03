package com.moigferdsrte.kaleidoscopehodgepodge.client.interaction;

import com.moigferdsrte.kaleidoscopehodgepodge.interaction.PackingBagRotationHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.network.RotatePackingBagPayload;
import com.moigferdsrte.kaleidoscopehodgepodge.network.BulkPlacePackingBagPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

@Environment(EnvType.CLIENT)
public final class PackingBagRotationClientHandler {
    public static void register() {
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (!(client.hitResult instanceof BlockHitResult hit)
                    || player == null
                    || !ClientPlayNetworking.canSend(RotatePackingBagPayload.TYPE)) {
                return false;
            }

            // 左键事件只处理旋转逻辑，不处理批量放置
            InteractionHand hand = PackingBagRotationHandler.canRotate(
                    player, player.level(), hit.getBlockPos(), InteractionHand.MAIN_HAND)
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (!PackingBagRotationHandler.canRotate(player, player.level(), hit.getBlockPos(), hand)) {
                return false;
            }

            // clickCount 只在新的按下沿大于零；按住时仅拦截挖掘，不重复发送旋转请求。
            if (clickCount > 0) {
                for (int click = 0; click < clickCount; click++) {
                    ClientPlayNetworking.send(new RotatePackingBagPayload(hit.getBlockPos(), hand));
                }
                player.swing(hand);
            }
            return true;
        });
    }

    private PackingBagRotationClientHandler() {
    }
}
