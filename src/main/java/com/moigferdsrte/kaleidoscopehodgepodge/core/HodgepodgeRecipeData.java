package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.Optional;
import java.util.UUID;

/** 杂烩配方持久化层数据 */
public record HodgepodgeRecipeData(String feastCode, UUID owner, Optional<GameProfile> ownerProfile,
                                   Optional<Component> dishName) {
    public static final Codec<HodgepodgeRecipeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("feast_code").forGetter(HodgepodgeRecipeData::feastCode),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("owner").forGetter(HodgepodgeRecipeData::owner),
            ExtraCodecs.STORED_GAME_PROFILE.codec().optionalFieldOf("owner_profile")
                    .forGetter(HodgepodgeRecipeData::ownerProfile),
            ComponentSerialization.CODEC.optionalFieldOf("dish_name").forGetter(HodgepodgeRecipeData::dishName)
    ).apply(instance, HodgepodgeRecipeData::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, HodgepodgeRecipeData> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, HodgepodgeRecipeData::feastCode,
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), HodgepodgeRecipeData::owner,
                    ByteBufCodecs.optional(ByteBufCodecs.GAME_PROFILE), HodgepodgeRecipeData::ownerProfile,
                    ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), HodgepodgeRecipeData::dishName,
                    HodgepodgeRecipeData::new);

    public HodgepodgeRecipeData(String feastCode, UUID owner) {
        this(feastCode, owner, Optional.empty());
    }

    public HodgepodgeRecipeData(String feastCode, UUID owner, Optional<GameProfile> ownerProfile) {
        this(feastCode, owner, ownerProfile, Optional.empty());
    }

    public HodgepodgeRecipeData(String feastCode, GameProfile ownerProfile) {
        this(feastCode, ownerProfile.id(), Optional.of(ownerProfile));
    }

    public HodgepodgeRecipeData(String feastCode, GameProfile ownerProfile, Optional<Component> dishName) {
        this(feastCode, ownerProfile.id(), Optional.of(ownerProfile), dishName);
    }

    public HodgepodgeRecipeData {
        dishName = dishName.filter(value -> !value.getString().isBlank()).map(Component::copy);
    }
}
