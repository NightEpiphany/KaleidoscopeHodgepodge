package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.item.IngredientDisplayItem;
import com.mojang.datafixers.util.Unit;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.HodgepodgeRecipeItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class KHItems {
    public static final Item HODGEPODGE_RECIPE = registerItemViaBlock(KHBlocks.HODGEPODGE_RECIPE,
            HodgepodgeRecipeItem::new, new Item.Properties());

    public static final Item WRAPPING_BAG = registerItem("wrapping_bag", WrappingBagItem::new, new Item.Properties());

    public static final Item LUNCH_BOX = registerItem("lunch_box", LunchBoxItem::new, new Item.Properties());

    public static final Item INGREDIENT_DISPLAY = registerItem("ingredient_display", IngredientDisplayItem::new,
            new Item.Properties().component(KHDataComponents.INGREDIENT_DISPLAY_MODEL, ""));

    public static final Item WOODEN_PLATE = registerItemViaBlock(
            KHBlocks.WOODEN_PLATE, CustomFeastBlockItem::new, new Item.Properties());

    public static final Item BAMBOO_DISPLAY_TRAY = registerItemViaBlock(
            KHBlocks.BAMBOO_DISPLAY_TRAY, CustomFeastBlockItem::new, new Item.Properties());

    public static final Item PORCELAIN_PLATE = registerItemViaBlock(
            KHBlocks.PORCELAIN_PLATE, CustomFeastBlockItem::new, new Item.Properties());

    public static final Item MEDIAN_PORCELAIN_PLATE = registerItemViaBlock(
            KHBlocks.MEDIAN_PORCELAIN_PLATE, CustomFeastBlockItem::new, new Item.Properties());

    public static final Item LARGE_PORCELAIN_PLATE = registerItemViaBlock(KHBlocks.LARGE_PORCELAIN_PLATE,
            (block, properties) -> new CustomFeastBlockItem(block, false, true, properties), new Item.Properties());

    public static final Item PORCELAIN_SOUP_BOWL = registerItemViaBlock(KHBlocks.PORCELAIN_SOUP_BOWL,
            ((block, properties) -> new CustomFeastBlockItem(block, true, false, properties)),
            new Item.Properties().component(KHDataComponents.SOUP_BASE, Unit.INSTANCE));

    public static Item registerItem(String string, Function<Item.Properties, Item> function, Item.Properties properties) {
        return registerItem(ResourceKey.create(Registries.ITEM, KaleidoscopeHodgepodge.id(string)), function, properties);
    }

    @SuppressWarnings("unused")
    public static Item registerItemViaBlock(Block block, Item.Properties properties) {
        return registerItemViaBlock(block, BlockItem::new, new Item.Properties());
    }

    @SuppressWarnings("deprecation")
    public static Item registerItemViaBlock(Block block, BiFunction<Block, Item.Properties, Item> biFunction, Item.Properties properties) {
        return registerItem(ResourceKey.create(Registries.ITEM, block.builtInRegistryHolder().key().identifier()),
                (properties2) -> biFunction.apply(block, properties2), properties.useBlockDescriptionPrefix());
    }

    public static Item registerItem(ResourceKey<Item> resourceKey, Function<Item.Properties, Item> function, Item.Properties properties) {
        Item item = function.apply(properties.setId(resourceKey));
        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }

        return Registry.register(BuiltInRegistries.ITEM, resourceKey, item);
    }

    public static void init() {
    }
}
