package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record IngredientStatusEffect(ResourceLocation id, int duration, int amplifier, boolean ambient,
                                     boolean visible, boolean showIcon) {
    public static final Codec<IngredientStatusEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(IngredientStatusEffect::id),
            Codec.INT.fieldOf("duration").forGetter(IngredientStatusEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(IngredientStatusEffect::amplifier),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(IngredientStatusEffect::ambient),
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(IngredientStatusEffect::visible),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(IngredientStatusEffect::showIcon)
    ).apply(instance, IngredientStatusEffect::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientStatusEffect> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, IngredientStatusEffect::id,
                    ByteBufCodecs.VAR_INT, IngredientStatusEffect::duration,
                    ByteBufCodecs.VAR_INT, IngredientStatusEffect::amplifier,
                    ByteBufCodecs.BOOL, IngredientStatusEffect::ambient,
                    ByteBufCodecs.BOOL, IngredientStatusEffect::visible,
                    ByteBufCodecs.BOOL, IngredientStatusEffect::showIcon,
                    IngredientStatusEffect::new);

    public IngredientStatusEffect {
        duration = duration == -1 ? -1 : Math.max(0, duration);
        amplifier = Math.max(0, Math.min(255, amplifier));
    }
}
