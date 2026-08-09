package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class KHMenus {
    public static final MenuType<LunchBoxMenu> LUNCH_BOX = Registry.register(
            BuiltInRegistries.MENU,
            KaleidoscopeHodgepodge.id("lunch_box"),
            new MenuType<>(LunchBoxMenu::new, FeatureFlags.VANILLA_SET));

    public static void init() {
    }

    private KHMenus() {
    }
}
