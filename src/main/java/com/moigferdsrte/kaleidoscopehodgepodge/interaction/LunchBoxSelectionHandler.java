package com.moigferdsrte.kaleidoscopehodgepodge.interaction;

import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.network.SelectLunchBoxIngredientPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class LunchBoxSelectionHandler {
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SelectLunchBoxIngredientPayload.TYPE,
                SelectLunchBoxIngredientPayload.STREAM_CODEC,
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
