package com.moigferdsrte.kaleidoscopehodgepodge.interaction;

import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.network.SelectLunchBoxIngredientPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class LunchBoxSelectionHandler {
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(SelectLunchBoxIngredientPayload.TYPE,
                SelectLunchBoxIngredientPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectLunchBoxIngredientPayload.TYPE,
                (payload, context) -> {
                    if (context.player().containerMenu instanceof LunchBoxMenu menu
                            && menu.containerId == payload.containerId()
                            && menu.ownsBox(context.player(), payload.hand())) {
                        menu.selectFromNetwork(payload.slotIndex(), context.player());
                    }
                });
    }

    private LunchBoxSelectionHandler() {}
}
