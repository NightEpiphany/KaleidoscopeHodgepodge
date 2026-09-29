package com.moigferdsrte.kaleidoscopehodgepodge.client.interaction;

import com.moigferdsrte.kaleidoscopehodgepodge.interaction.PackingBagRotationHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.network.RotatePackingBagPayload;
import com.moigferdsrte.kaleidoscopehodgepodge.network.BulkPlacePackingBagPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

@OnlyIn(Dist.CLIENT)
public final class PackingBagRotationClientHandler {
    private static boolean rotatedWhileHeld;

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (!Minecraft.getInstance().options.keyAttack.isDown()) rotatedWhileHeld = false;
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
            for (InteractionHand hand : InteractionHand.values()) {
                if (PackingBagRotationHandler.canRotate(event.getEntity(), event.getLevel(), event.getPos(), hand)) {
                    event.setCanceled(true);
                    return;
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (!event.isAttack()) return;
            Minecraft client = Minecraft.getInstance();
            var player = client.player;
            if (!(client.hitResult instanceof BlockHitResult hit)
                    || player == null) {
                return;
            }

            // 左键事件只处理旋转逻辑，不处理批量放置
            InteractionHand hand = PackingBagRotationHandler.canRotate(
                    player, player.level(), hit.getBlockPos(), InteractionHand.MAIN_HAND)
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (!PackingBagRotationHandler.canRotate(player, player.level(), hit.getBlockPos(), hand)) {
                return;
            }

            // clickCount 只在新的按下沿大于零；按住时仅拦截挖掘，不重复发送旋转请求。
            if (!rotatedWhileHeld) {
                PacketDistributor.sendToServer(new RotatePackingBagPayload(hit.getBlockPos(), hand));
                rotatedWhileHeld = true;
                player.swing(hand);
            }
            event.setSwingHand(false);
            event.setCanceled(true);
        });
    }

    private PackingBagRotationClientHandler() {
    }
}
