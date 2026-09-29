package com.moigferdsrte.kaleidoscopehodgepodge.network;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

/*旋转食材模型的payload请求体*/
public record RotatePackingBagPayload(BlockPos pos, InteractionHand hand) implements CustomPacketPayload {
    public RotatePackingBagPayload(BlockPos pos) {
        this(pos, InteractionHand.MAIN_HAND);
    }
    public static final Type<RotatePackingBagPayload> TYPE =
            new Type<>(KaleidoscopeHodgepodge.id("rotate_packing_bag"));
    public static final StreamCodec<ByteBuf, RotatePackingBagPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RotatePackingBagPayload::pos,
            ByteBufCodecs.VAR_INT.map(value -> value == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, InteractionHand::ordinal), RotatePackingBagPayload::hand,
            RotatePackingBagPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
