package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.mojang.datafixers.util.Unit;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class KHItems {

    private static final ResourceKey<CreativeModeTab> COOKERY_MAIN_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "cookery_main"));

    public static final Item WRAPPING_BAG = registerItem("wrapping_bag", WrappingBagItem::new, new Item.Properties());

    public static final Item LUNCH_BOX = registerItem("lunch_box", LunchBoxItem::new, new Item.Properties());

    public static final Item INGREDIENT_DISPLAY = registerItem("ingredient_display", Item::new,
            new Item.Properties().stacksTo(1).component(KHDataComponents.INGREDIENT_DISPLAY_MODEL, ""));

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
        return registerItem(ResourceKey.create(Registries.ITEM, block.builtInRegistryHolder().key().location()),
                properties2 -> biFunction.apply(block, properties2), properties);
    }

    public static Item registerItem(ResourceKey<Item> resourceKey, Function<Item.Properties, Item> function, Item.Properties properties) {
        Item item = function.apply(properties);
        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }

        return Registry.register(BuiltInRegistries.ITEM, resourceKey, item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(COOKERY_MAIN_TAB).register(output -> {
            output.addAfter(ModItems.TRASH_CAN, LUNCH_BOX);
            output.addAfter(ModItems.FRUIT_BASKET, WRAPPING_BAG);
            output.addAfter(WRAPPING_BAG, WOODEN_PLATE);
            output.addAfter(WOODEN_PLATE, BAMBOO_DISPLAY_TRAY);
            output.addAfter(BAMBOO_DISPLAY_TRAY, PORCELAIN_PLATE);
            output.addAfter(PORCELAIN_PLATE, MEDIAN_PORCELAIN_PLATE);
            output.addAfter(MEDIAN_PORCELAIN_PLATE, LARGE_PORCELAIN_PLATE);
            output.addAfter(LARGE_PORCELAIN_PLATE, PORCELAIN_SOUP_BOWL);
        });
    }
}
