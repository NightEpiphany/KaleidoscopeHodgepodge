package com.moigferdsrte.kaleidoscopehodgepodge.network;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** 放置食材模型的payload请求体 */
public record BulkPlacePackingBagPayload(BlockPos pos, Vec3 location, Direction direction,
                                         boolean inside, InteractionHand hand)
        implements CustomPacketPayload {
    public static final Type<BulkPlacePackingBagPayload> TYPE =
            new Type<>(KaleidoscopeHodgepodge.id("bulk_place_packing_bag"));
    public static final StreamCodec<ByteBuf, BulkPlacePackingBagPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BulkPlacePackingBagPayload::pos,
            StreamCodec.composite(ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y,
                    ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new), BulkPlacePackingBagPayload::location,
            Direction.STREAM_CODEC, BulkPlacePackingBagPayload::direction,
            ByteBufCodecs.BOOL, BulkPlacePackingBagPayload::inside,
            ByteBufCodecs.VAR_INT.map(value -> value == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, InteractionHand::ordinal), BulkPlacePackingBagPayload::hand,
            BulkPlacePackingBagPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
