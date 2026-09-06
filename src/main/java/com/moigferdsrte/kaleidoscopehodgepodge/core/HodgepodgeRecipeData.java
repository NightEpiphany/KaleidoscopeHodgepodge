package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** 杂烩配方持久化层数据 */
public record HodgepodgeRecipeData(String feastCode, UUID owner) {
    public static final Codec<HodgepodgeRecipeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("feast_code").forGetter(HodgepodgeRecipeData::feastCode),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("owner").forGetter(HodgepodgeRecipeData::owner)
    ).apply(instance, HodgepodgeRecipeData::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, HodgepodgeRecipeData> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, HodgepodgeRecipeData::feastCode,
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), HodgepodgeRecipeData::owner,
                    HodgepodgeRecipeData::new);
}
