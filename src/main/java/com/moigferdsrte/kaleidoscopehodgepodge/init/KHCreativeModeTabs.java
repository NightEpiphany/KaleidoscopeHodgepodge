package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KHCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KaleidoscopeHodgepodge.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("item_group.kaleidoscope_hodgepodge.main"))
                    .icon(() -> filledBag(PackingIngredients.values()[0]))
                    .displayItems((parameters, output) -> {
                        for (PackingIngredients ingredient : PackingIngredients.values()) {
                            boolean missingOptionalMod = ingredient.getSrcFoodIds().stream().anyMatch(id ->
                                    id.getNamespace().equals("kaleidoscope_nether")
                                            && !ModList.get().isLoaded("kaleidoscope_nether")
                                            || id.getNamespace().equals("kaleidoscope_end")
                                            && !ModList.get().isLoaded("kaleidoscope_end"));
                            if (!missingOptionalMod) output.accept(filledBag(ingredient));
                        }
                    })
                    .build());

    private static ItemStack filledBag(PackingIngredients ingredient) {
        ItemStack stack = KHItems.WRAPPING_BAG.get().getDefaultInstance();
        PackingBagService.set(stack, PackingBagContents.single(new BaggedIngredient(ingredient.getId())));
        return stack;
    }

    public static void init(IEventBus modBus) {
        TABS.register(modBus);
    }

    private KHCreativeModeTabs() {}
}
