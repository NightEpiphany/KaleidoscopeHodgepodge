package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Immutable ingredient placement in block-local pixel coordinates. */
public record PlacedIngredient(ResourceLocation id, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                               int rotation, IngredientFoodData food) {
    public static final Codec<PlacedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(PlacedIngredient::id),
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

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedIngredient> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public @NotNull PlacedIngredient decode(RegistryFriendlyByteBuf buffer) {
                    return new PlacedIngredient(
                            ResourceLocation.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readVarInt(), IngredientFoodData.STREAM_CODEC.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, PlacedIngredient value) {
                    ResourceLocation.STREAM_CODEC.encode(buffer, value.id());
                    buffer.writeVarInt(value.x());
                    buffer.writeVarInt(value.y());
                    buffer.writeVarInt(value.z());
                    buffer.writeVarInt(value.sizeX());
                    buffer.writeVarInt(value.sizeY());
                    buffer.writeVarInt(value.sizeZ());
                    buffer.writeVarInt(value.rotation());
                    IngredientFoodData.STREAM_CODEC.encode(buffer, value.food());
                }
            };

    public PlacedIngredient {
        rotation = Math.floorMod(rotation, 4);
    }

    public PlacedIngredient(ResourceLocation id, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        this(id, x, y, z, sizeX, sizeY, sizeZ, 0, IngredientFoodData.EMPTY);
    }

    public PlacedIngredient(ResourceLocation id, int x, int y, int z, int sizeX, int sizeY, int sizeZ, int rotation) {
        this(id, x, y, z, sizeX, sizeY, sizeZ, rotation, IngredientFoodData.EMPTY);
    }

    public PlacedIngredient withFood(IngredientFoodData food) {
        return new PlacedIngredient(id, x, y, z, sizeX, sizeY, sizeZ, rotation, food);
    }

    public PlacedIngredient translated(int offsetX, int offsetZ) {
        return new PlacedIngredient(id, x + offsetX, y, z + offsetZ,
                sizeX, sizeY, sizeZ, rotation, food);
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
