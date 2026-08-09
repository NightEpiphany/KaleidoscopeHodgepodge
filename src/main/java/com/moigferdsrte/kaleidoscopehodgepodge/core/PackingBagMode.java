package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum PackingBagMode {
    PLACEMENT("placement"),
    STORAGE("storage");

    public static final Codec<PackingBagMode> CODEC = Codec.STRING.xmap(PackingBagMode::byName, mode -> mode.name);
    public static final StreamCodec<RegistryFriendlyByteBuf, PackingBagMode> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PackingBagMode::ordinal, PackingBagMode::byOrdinal);

    private final String name;

    PackingBagMode(String name) {
        this.name = name;
    }

    public PackingBagMode next() {
        return this == PLACEMENT ? STORAGE : PLACEMENT;
    }

    public String translationKey() {
        return "tooltip.kaleidoscope_hodgepodge.bag_mode_" + name;
    }

    private static PackingBagMode byName(String name) {
        for (PackingBagMode mode : values()) {
            if (mode.name.equals(name)) return mode;
        }
        return PLACEMENT;
    }

    private static PackingBagMode byOrdinal(int ordinal) {
        PackingBagMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PLACEMENT;
    }
}
