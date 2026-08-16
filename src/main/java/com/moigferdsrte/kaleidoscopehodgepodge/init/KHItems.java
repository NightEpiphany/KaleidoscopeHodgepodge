package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.mojang.datafixers.util.Unit;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class KHItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KaleidoscopeHodgepodge.MOD_ID);
    private static final ResourceKey<CreativeModeTab> COOKERY_MAIN_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "cookery_main"));

    public static final DeferredItem<WrappingBagItem> WRAPPING_BAG =
            ITEMS.registerItem("wrapping_bag", WrappingBagItem::new);

    public static final DeferredItem<LunchBoxItem> LUNCH_BOX =
            ITEMS.registerItem("lunch_box", LunchBoxItem::new);

    public static final DeferredItem<Item> INGREDIENT_DISPLAY = ITEMS.register("ingredient_display", () ->
            new Item(new Item.Properties().stacksTo(1)
                    .component(KHDataComponents.INGREDIENT_DISPLAY_MODEL.get(), "")));

    public static final DeferredItem<CustomFeastBlockItem> WOODEN_PLATE = registerBlockItem(
            "wooden_plate", KHBlocks.WOODEN_PLATE, CustomFeastBlockItem::new, Item.Properties::new);

    public static final DeferredItem<CustomFeastBlockItem> PORCELAIN_PLATE = registerBlockItem(
            "porcelain_plate", KHBlocks.PORCELAIN_PLATE, CustomFeastBlockItem::new, Item.Properties::new);

    public static final DeferredItem<CustomFeastBlockItem> MEDIAN_PORCELAIN_PLATE = registerBlockItem(
            "median_porcelain_plate", KHBlocks.MEDIAN_PORCELAIN_PLATE,
            CustomFeastBlockItem::new, Item.Properties::new);

    public static final DeferredItem<CustomFeastBlockItem> LARGE_PORCELAIN_PLATE = registerBlockItem(
            "large_porcelain_plate", KHBlocks.LARGE_PORCELAIN_PLATE,
            (block, properties) -> new CustomFeastBlockItem(block, false, true, properties),
            Item.Properties::new);

    public static final DeferredItem<CustomFeastBlockItem> PORCELAIN_SOUP_BOWL = registerBlockItem(
            "porcelain_soup_bowl", KHBlocks.PORCELAIN_SOUP_BOWL,
            (block, properties) -> new CustomFeastBlockItem(block, true, false, properties),
            () -> new Item.Properties().component(KHDataComponents.SOUP_BASE.get(), Unit.INSTANCE));

    private static <T extends Item> DeferredItem<T> registerBlockItem(
            String name, Supplier<? extends Block> block,
            BiFunction<Block, Item.Properties, T> factory, Supplier<Item.Properties> properties) {
        return ITEMS.register(name, () -> factory.apply(block.get(), properties.get()));
    }

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(KHItems::addCreativeTabEntries);
    }

    private static void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(COOKERY_MAIN_TAB)) return;
        CreativeModeTab.TabVisibility both = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        event.insertAfter(ModItems.TRASH_CAN.get().getDefaultInstance(), LUNCH_BOX.get().getDefaultInstance(), both);
        event.insertAfter(ModItems.FRUIT_BASKET.get().getDefaultInstance(), WRAPPING_BAG.get().getDefaultInstance(), both);
        event.insertAfter(WRAPPING_BAG.get().getDefaultInstance(), WOODEN_PLATE.get().getDefaultInstance(), both);
        event.insertAfter(WOODEN_PLATE.get().getDefaultInstance(), PORCELAIN_PLATE.get().getDefaultInstance(), both);
        event.insertAfter(PORCELAIN_PLATE.get().getDefaultInstance(), MEDIAN_PORCELAIN_PLATE.get().getDefaultInstance(), both);
        event.insertAfter(MEDIAN_PORCELAIN_PLATE.get().getDefaultInstance(), LARGE_PORCELAIN_PLATE.get().getDefaultInstance(), both);
        event.insertAfter(LARGE_PORCELAIN_PLATE.get().getDefaultInstance(), PORCELAIN_SOUP_BOWL.get().getDefaultInstance(), both);
    }

    private KHItems() {}
}
