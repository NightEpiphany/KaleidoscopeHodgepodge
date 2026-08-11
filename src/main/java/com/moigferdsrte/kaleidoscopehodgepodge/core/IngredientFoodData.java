package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** 单个材料对应的原菜品每口营养与药水效果快照。 */
public record IngredientFoodData(int nutrition, float saturation,
                                 List<IngredientEffectGroup> effects) {
    private static final int MAX_EFFECT_GROUPS = 16;
    public static final IngredientFoodData EMPTY = new IngredientFoodData(0, 0.0F, List.of());
    public static final Codec<IngredientFoodData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("nutrition").forGetter(IngredientFoodData::nutrition),
            Codec.FLOAT.fieldOf("saturation").forGetter(IngredientFoodData::saturation),
            IngredientEffectGroup.CODEC.sizeLimitedListOf(MAX_EFFECT_GROUPS)
                    .optionalFieldOf("effects", List.of()).forGetter(IngredientFoodData::effects)
    ).apply(instance, IngredientFoodData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientFoodData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, IngredientFoodData::nutrition,
                    ByteBufCodecs.FLOAT, IngredientFoodData::saturation,
                    IngredientEffectGroup.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_EFFECT_GROUPS)),
                    IngredientFoodData::effects,
                    IngredientFoodData::new);

    public IngredientFoodData {
        nutrition = Math.max(0, nutrition);
        saturation = Float.isFinite(saturation) ? Math.max(0.0F, saturation) : 0.0F;
        effects = List.copyOf(effects.subList(0, Math.min(effects.size(), MAX_EFFECT_GROUPS)));
    }

    public boolean isEmpty() {
        return nutrition == 0 && saturation == 0.0F && effects.isEmpty();
    }

    public IngredientFoodData multiplyNutrition(int multiplier) {
        if (multiplier <= 1 || nutrition == 0) return this;
        int scaled = (int) Math.min(Integer.MAX_VALUE, (long) nutrition * multiplier);
        return new IngredientFoodData(scaled, saturation, effects);
    }
}
