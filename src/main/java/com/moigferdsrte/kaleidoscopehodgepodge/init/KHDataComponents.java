package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KHDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, KaleidoscopeHodgepodge.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PACKING_BAG_INGREDIENT =
            COMPONENTS.registerComponentType("packing_bag_ingredient", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PackingBagContents>> PACKING_BAG_CONTENTS =
            COMPONENTS.registerComponentType("packing_bag_contents", builder -> builder
                    .persistent(PackingBagContents.CODEC).networkSynchronized(PackingBagContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PackingBagMode>> PACKING_BAG_MODE =
            COMPONENTS.registerComponentType("packing_bag_mode", builder -> builder
                    .persistent(PackingBagMode.CODEC).networkSynchronized(PackingBagMode.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> INGREDIENT_DISPLAY_MODEL =
            COMPONENTS.registerComponentType("ingredient_display_model", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomFeastData>> CUSTOM_FEAST =
            COMPONENTS.registerComponentType("custom_feast", builder -> builder
                    .persistent(CustomFeastData.CODEC).networkSynchronized(CustomFeastData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SOUP_BASE =
            COMPONENTS.registerComponentType("soup_base", builder -> builder
                    .persistent(Codec.BOOL.xmap(ignored -> Unit.INSTANCE, ignored -> true))
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static void init(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    private KHDataComponents() {}
}
