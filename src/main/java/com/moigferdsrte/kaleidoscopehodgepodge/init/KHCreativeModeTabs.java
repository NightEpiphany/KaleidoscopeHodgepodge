package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import static com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems.*;

public final class KHCreativeModeTabs {

    private static final ResourceKey<CreativeModeTab> COOKERY_MAIN_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "cookery_main"));
    private static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, KaleidoscopeHodgepodge.id("main"));

    @SuppressWarnings("unused")
    public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("item_group.kaleidoscope_hodgepodge.main"))
                    .icon(() -> filledBag(PackingIngredients.values()[0]))
                    .displayItems((_, output) -> {
                        for (PackingIngredients ingredient : PackingIngredients.values()) {
                            if (
                                    ingredient.getSrcFoodIds().stream().anyMatch(s -> s.getNamespace().equals("kaleidoscope_nether")) && !FabricLoader.getInstance().isModLoaded("kaleidoscope_nether")
                                    || ingredient.getSrcFoodIds().stream().anyMatch(s -> s.getNamespace().equals("kaleidoscope_end")) && !FabricLoader.getInstance().isModLoaded("kaleidoscope_end")
                            )
                                continue;
                            output.accept(filledBag(ingredient));
                        }
                    })
                    .build());

    private static ItemStack filledBag(PackingIngredients ingredient) {
        ItemStack stack = WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(stack, PackingBagContents.single(new BaggedIngredient(ingredient.getId())));
        return stack;
    }

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(COOKERY_MAIN_TAB).register(output -> {
            output.accept(LUNCH_BOX);
            for (DyeColor color : DyeColor.values()) {
                output.accept(LunchBoxItem.colored(color));
            }
            output.insertAfter(ModItems.FRUIT_BASKET, WRAPPING_BAG);
            output.insertAfter(WRAPPING_BAG, WOODEN_PLATE);
            output.insertAfter(WOODEN_PLATE, BAMBOO_DISPLAY_TRAY);
            output.insertAfter(BAMBOO_DISPLAY_TRAY, PORCELAIN_PLATE);
            output.insertAfter(PORCELAIN_PLATE, MEDIAN_PORCELAIN_PLATE);
            output.insertAfter(MEDIAN_PORCELAIN_PLATE, LARGE_PORCELAIN_PLATE);
            output.insertAfter(LARGE_PORCELAIN_PLATE, PORCELAIN_SOUP_BOWL);
        });
    }

    private KHCreativeModeTabs() {
    }
}
