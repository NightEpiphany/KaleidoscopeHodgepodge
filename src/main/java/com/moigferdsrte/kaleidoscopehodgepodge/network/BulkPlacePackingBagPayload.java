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
import org.jspecify.annotations.NonNull;

/** Requests repeated placement of the current wrapping-bag contents. */
public record BulkPlacePackingBagPayload(BlockPos pos, Vec3 location, Direction direction,
                                         boolean inside, InteractionHand hand)
        implements CustomPacketPayload {
    public static final Type<BulkPlacePackingBagPayload> TYPE =
            new Type<>(KaleidoscopeHodgepodge.id("bulk_place_packing_bag"));
    public static final StreamCodec<ByteBuf, BulkPlacePackingBagPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BulkPlacePackingBagPayload::pos,
            Vec3.STREAM_CODEC, BulkPlacePackingBagPayload::location,
            Direction.STREAM_CODEC, BulkPlacePackingBagPayload::direction,
            ByteBufCodecs.BOOL, BulkPlacePackingBagPayload::inside,
            InteractionHand.STREAM_CODEC, BulkPlacePackingBagPayload::hand,
            BulkPlacePackingBagPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
