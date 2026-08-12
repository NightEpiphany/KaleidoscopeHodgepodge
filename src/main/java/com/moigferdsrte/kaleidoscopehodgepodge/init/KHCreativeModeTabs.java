package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class KHCreativeModeTabs {
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
        ItemStack stack = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(stack, PackingBagContents.single(new BaggedIngredient(ingredient.getId())));
        return stack;
    }

    public static void init() {

    }

    private KHCreativeModeTabs() {
    }
}
