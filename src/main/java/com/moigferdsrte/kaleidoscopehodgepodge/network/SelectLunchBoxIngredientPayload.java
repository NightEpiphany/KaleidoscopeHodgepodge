package com.moigferdsrte.kaleidoscopehodgepodge.network;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.NonNull;

/*选择午餐盒槽位的payload请求体*/
public record SelectLunchBoxIngredientPayload(int containerId, int slotIndex, InteractionHand hand)
        implements CustomPacketPayload {
    public static final Type<SelectLunchBoxIngredientPayload> TYPE =
            new Type<>(KaleidoscopeHodgepodge.id("select_lunch_box_ingredient"));
    public static final StreamCodec<ByteBuf, SelectLunchBoxIngredientPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectLunchBoxIngredientPayload::containerId,
            ByteBufCodecs.VAR_INT, SelectLunchBoxIngredientPayload::slotIndex,
            InteractionHand.STREAM_CODEC, SelectLunchBoxIngredientPayload::hand,
            SelectLunchBoxIngredientPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
