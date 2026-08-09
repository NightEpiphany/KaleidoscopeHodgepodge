package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class KHDataComponents {
    public static final DataComponentType<String> PACKING_BAG_INGREDIENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("packing_bag_ingredient"),
            DataComponentType.<String>builder().persistent(com.mojang.serialization.Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    public static final DataComponentType<PackingBagContents> PACKING_BAG_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("packing_bag_contents"),
            DataComponentType.<PackingBagContents>builder().persistent(PackingBagContents.CODEC)
                    .networkSynchronized(PackingBagContents.STREAM_CODEC).build());

    public static final DataComponentType<PackingBagMode> PACKING_BAG_MODE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("packing_bag_mode"),
            DataComponentType.<PackingBagMode>builder().persistent(PackingBagMode.CODEC)
                    .networkSynchronized(PackingBagMode.STREAM_CODEC).build());

    public static final DataComponentType<String> INGREDIENT_DISPLAY_MODEL = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("ingredient_display_model"),
            DataComponentType.<String>builder().persistent(com.mojang.serialization.Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    public static final DataComponentType<CustomFeastData> CUSTOM_FEAST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("custom_feast"),
            DataComponentType.<CustomFeastData>builder().persistent(CustomFeastData.CODEC)
                    .networkSynchronized(CustomFeastData.STREAM_CODEC).build());

    public static void init() {}

    private KHDataComponents() {}
}
