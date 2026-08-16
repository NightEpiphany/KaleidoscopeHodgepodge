package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KHMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KaleidoscopeHodgepodge.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LunchBoxMenu>> LUNCH_BOX =
            MENUS.register("lunch_box", () -> new MenuType<>(LunchBoxMenu::new, FeatureFlags.VANILLA_SET));

    public static void init(IEventBus modBus) {
        MENUS.register(modBus);
    }

    private KHMenus() {}
}
