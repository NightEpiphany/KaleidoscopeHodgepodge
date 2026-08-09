package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** 纸袋中的一个材料模型、旋转象限及其每口食用数据。 */
public record BaggedIngredient(Identifier id, int rotation, IngredientFoodData food) {
    public static final Codec<BaggedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(BaggedIngredient::id),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(BaggedIngredient::rotation),
            IngredientFoodData.CODEC.optionalFieldOf("food", IngredientFoodData.EMPTY)
                    .forGetter(BaggedIngredient::food)
    ).apply(instance, BaggedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BaggedIngredient> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, BaggedIngredient::id,
            ByteBufCodecs.VAR_INT, BaggedIngredient::rotation,
            IngredientFoodData.STREAM_CODEC, BaggedIngredient::food,
            BaggedIngredient::new);

    public BaggedIngredient {
        rotation = Math.floorMod(rotation, 4);
    }

    public BaggedIngredient(Identifier id) {
        this(id, 0, IngredientFoodData.EMPTY);
    }

    public BaggedIngredient(Identifier id, int rotation) {
        this(id, rotation, IngredientFoodData.EMPTY);
    }

    public BaggedIngredient rotateClockwise() {
        return new BaggedIngredient(id, rotation + 1, food);
    }
}
