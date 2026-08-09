package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record IngredientEffectGroup(float probability, List<IngredientStatusEffect> effects) {
    private static final int MAX_EFFECTS = 16;
    public static final Codec<IngredientEffectGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("probability").forGetter(IngredientEffectGroup::probability),
            IngredientStatusEffect.CODEC.sizeLimitedListOf(MAX_EFFECTS).fieldOf("effects")
                    .forGetter(IngredientEffectGroup::effects)
    ).apply(instance, IngredientEffectGroup::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientEffectGroup> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, IngredientEffectGroup::probability,
                    IngredientStatusEffect.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_EFFECTS)),
                    IngredientEffectGroup::effects,
                    IngredientEffectGroup::new);

    public IngredientEffectGroup {
        probability = Float.isFinite(probability) ? Math.max(0.0F, Math.min(1.0F, probability)) : 0.0F;
        effects = List.copyOf(effects.subList(0, Math.min(effects.size(), MAX_EFFECTS)));
    }
}
