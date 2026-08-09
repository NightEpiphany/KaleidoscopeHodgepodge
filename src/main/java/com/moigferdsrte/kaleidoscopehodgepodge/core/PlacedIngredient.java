package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;

/** Immutable ingredient placement in block-local pixel coordinates. */
public record PlacedIngredient(Identifier id, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                               int rotation, IngredientFoodData food) {
    public static final Codec<PlacedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(PlacedIngredient::id),
            Codec.INT.fieldOf("x").forGetter(PlacedIngredient::x),
            Codec.INT.fieldOf("y").forGetter(PlacedIngredient::y),
            Codec.INT.fieldOf("z").forGetter(PlacedIngredient::z),
            Codec.INT.fieldOf("size_x").forGetter(PlacedIngredient::sizeX),
            Codec.INT.fieldOf("size_y").forGetter(PlacedIngredient::sizeY),
            Codec.INT.fieldOf("size_z").forGetter(PlacedIngredient::sizeZ),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(PlacedIngredient::rotation),
            IngredientFoodData.CODEC.optionalFieldOf("food", IngredientFoodData.EMPTY)
                    .forGetter(PlacedIngredient::food)
    ).apply(instance, PlacedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedIngredient> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, PlacedIngredient::id,
            ByteBufCodecs.VAR_INT, PlacedIngredient::x,
            ByteBufCodecs.VAR_INT, PlacedIngredient::y,
            ByteBufCodecs.VAR_INT, PlacedIngredient::z,
            ByteBufCodecs.VAR_INT, PlacedIngredient::sizeX,
            ByteBufCodecs.VAR_INT, PlacedIngredient::sizeY,
            ByteBufCodecs.VAR_INT, PlacedIngredient::sizeZ,
            ByteBufCodecs.VAR_INT, PlacedIngredient::rotation,
            IngredientFoodData.STREAM_CODEC, PlacedIngredient::food,
            PlacedIngredient::new
    );

    public PlacedIngredient {
        rotation = Math.floorMod(rotation, 4);
    }

    public PlacedIngredient(Identifier id, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        this(id, x, y, z, sizeX, sizeY, sizeZ, 0, IngredientFoodData.EMPTY);
    }

    public PlacedIngredient(Identifier id, int x, int y, int z, int sizeX, int sizeY, int sizeZ, int rotation) {
        this(id, x, y, z, sizeX, sizeY, sizeZ, rotation, IngredientFoodData.EMPTY);
    }

    public PlacedIngredient withFood(IngredientFoodData food) {
        return new PlacedIngredient(id, x, y, z, sizeX, sizeY, sizeZ, rotation, food);
    }

    public boolean intersects(PlacedIngredient other) {
        return xMin() < other.xMax() && xMax() > other.xMin()
                && y < other.y + other.sizeY && y + sizeY > other.y
                && zMin() < other.zMax() && zMax() > other.zMin();
    }

    public int xMin() { return 2 * x - sizeX; }
    public int xMax() { return 2 * x + sizeX; }
    public int zMin() { return 2 * z - sizeZ; }
    public int zMax() { return 2 * z + sizeZ; }
}
