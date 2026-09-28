package com.moigferdsrte.kaleidoscopehodgepodge.util;

import com.mojang.serialization.Codec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.Optional;

/** Registry-aware codec persistence for block entities on Minecraft 1.21.1. */
public final class NbtCodecs {
    public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec,
                                      HolderLookup.Provider registries) {
        if (!tag.contains(key)) return Optional.empty();
        return codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(key))
                .resultOrPartial(error -> KaleidoscopeHodgepodge.LOGGER.warn("Invalid {}: {}", key, error));
    }

    public static <T> void write(CompoundTag tag, String key, Codec<T> codec, T value,
                                 HolderLookup.Provider registries) {
        codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value)
                .resultOrPartial(error -> KaleidoscopeHodgepodge.LOGGER.error("Unable to save {}: {}", key, error))
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    private NbtCodecs() {}
}
