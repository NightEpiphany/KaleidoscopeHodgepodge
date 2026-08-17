package com.moigferdsrte.kaleidoscopehodgepodge.network;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RotatePackingBagPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RotatePackingBagPayload> TYPE =
            new Type<>(KaleidoscopeHodgepodge.id("rotate_packing_bag"));
    public static final StreamCodec<ByteBuf, RotatePackingBagPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RotatePackingBagPayload::pos,
            RotatePackingBagPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
