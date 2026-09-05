package com.moigferdsrte.kaleidoscopehodgepodge.network;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.NonNull;

/*旋转食材模型的payload请求体*/
public record RotatePackingBagPayload(BlockPos pos, InteractionHand hand) implements CustomPacketPayload {
    public RotatePackingBagPayload(BlockPos pos) {
        this(pos, InteractionHand.MAIN_HAND);
    }
    public static final Type<RotatePackingBagPayload> TYPE =
            new Type<>(KaleidoscopeHodgepodge.id("rotate_packing_bag"));
    public static final StreamCodec<ByteBuf, RotatePackingBagPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RotatePackingBagPayload::pos,
            InteractionHand.STREAM_CODEC, RotatePackingBagPayload::hand,
            RotatePackingBagPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
