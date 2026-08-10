package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record CustomFeastData(ContainerKind kind, Direction facing, List<PlacedIngredient> ingredients) {
    public static final Codec<CustomFeastData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ContainerKind.CODEC.fieldOf("kind").forGetter(CustomFeastData::kind),
            Direction.CODEC.fieldOf("facing").forGetter(CustomFeastData::facing),
            PlacedIngredient.CODEC.sizeLimitedListOf(360).fieldOf("ingredients").forGetter(CustomFeastData::ingredients)
    ).apply(instance, CustomFeastData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomFeastData> STREAM_CODEC = StreamCodec.composite(
            ContainerKind.STREAM_CODEC, CustomFeastData::kind,
            Direction.STREAM_CODEC, CustomFeastData::facing,
            PlacedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(360)), CustomFeastData::ingredients,
            CustomFeastData::new
    );

    public CustomFeastData {
        ingredients = List.copyOf(ingredients);
    }

    public enum ContainerKind {
        DISH,
        SOUP
        ;

        public static final Codec<ContainerKind> CODEC = Codec.STRING.xmap(
                value -> value.equalsIgnoreCase("soup") ? SOUP : DISH,
                value -> value.name().toLowerCase()
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ContainerKind> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ContainerKind decode(RegistryFriendlyByteBuf buffer) {
                int value = buffer.readVarInt();
                return values()[Math.max(0, Math.min(values().length - 1, value))];
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ContainerKind value) {
                buffer.writeVarInt(value.ordinal());
            }
        };
    }
}
