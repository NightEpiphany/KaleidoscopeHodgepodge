package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.mojang.datafixers.util.Unit;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxContents;
import com.mojang.serialization.Codec;
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

    public static final DataComponentType<LunchBoxContents> LUNCH_BOX_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("lunch_box_contents"),
            DataComponentType.<LunchBoxContents>builder().persistent(LunchBoxContents.CODEC)
                    .networkSynchronized(LunchBoxContents.STREAM_CODEC).build());

    public static final DataComponentType<PackingBagMode> LUNCH_BOX_MODE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("lunch_box_mode"),
            DataComponentType.<PackingBagMode>builder().persistent(PackingBagMode.CODEC)
                    .networkSynchronized(PackingBagMode.STREAM_CODEC).build());

    public static final DataComponentType<Integer> LUNCH_BOX_SELECTED_SLOT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("lunch_box_selected_slot"),
            DataComponentType.<Integer>builder().persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DataComponentType<String> INGREDIENT_DISPLAY_MODEL = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("ingredient_display_model"),
            DataComponentType.<String>builder().persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    public static final DataComponentType<Integer> INGREDIENT_DISPLAY_ROTATION = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("ingredient_display_rotation"),
            DataComponentType.<Integer>builder().persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DataComponentType<IngredientFoodData> INGREDIENT_DISPLAY_FOOD = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("ingredient_display_food"),
            DataComponentType.<IngredientFoodData>builder().persistent(IngredientFoodData.CODEC)
                    .networkSynchronized(IngredientFoodData.STREAM_CODEC).build());

    public static final DataComponentType<CustomFeastData> CUSTOM_FEAST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("custom_feast"),
            DataComponentType.<CustomFeastData>builder().persistent(CustomFeastData.CODEC)
                    .networkSynchronized(CustomFeastData.STREAM_CODEC).build());

    /** 标记瓷汤碗是否仍保留汤底。 */
    public static final DataComponentType<Unit> SOUP_BASE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            KaleidoscopeHodgepodge.id("soup_base"),
            DataComponentType.<Unit>builder()
                    .persistent(Codec.BOOL.xmap(ignored -> Unit.INSTANCE, ignored -> true))
                    .networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(Unit.INSTANCE))
                    .build());

    public static void init() {}

    private KHDataComponents() {}
}
