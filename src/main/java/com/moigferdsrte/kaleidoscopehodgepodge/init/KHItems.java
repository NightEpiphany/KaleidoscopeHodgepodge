package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.item.DishBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class KHItems {

    public static final Item WRAPPING_BAG = registerItem("wrapping_bag", WrappingBagItem::new, new Item.Properties());

    public static final Item WOODEN_PLATE = registerItemViaBlock(KHBlocks.WOODEN_PLATE, DishBlockItem::new, new Item.Properties());

    public static final Item PORCELAIN_PLATE = registerItemViaBlock(KHBlocks.PORCELAIN_PLATE, DishBlockItem::new, new Item.Properties());

    public static Item registerItem(String string, Function<Item.Properties, Item> function, Item.Properties properties) {
        return registerItem(ResourceKey.create(Registries.ITEM, KaleidoscopeHodgepodge.id(string)), function, properties);
    }

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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            output.accept(WRAPPING_BAG);
            output.accept(WOODEN_PLATE);
            output.accept(PORCELAIN_PLATE);
        });
    }
}
