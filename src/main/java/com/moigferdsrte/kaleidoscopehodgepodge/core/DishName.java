package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/*菜品自定义名称*/
public final class DishName {
    private DishName() {}

    public static Optional<Component> get(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) name = stack.get(KHDataComponents.DISH_NAME);
        return Optional.ofNullable(name).filter(value -> !value.getString().isBlank()).map(Component::copy);
    }

    public static void set(ItemStack stack, Optional<Component> name) {
        Optional<Component> valid = name.filter(value -> !value.getString().isBlank());
        if (valid.isPresent()) {
            stack.set(KHDataComponents.DISH_NAME, valid.get().copy());
            stack.set(DataComponents.CUSTOM_NAME, valid.get().copy());
        } else {
            stack.remove(KHDataComponents.DISH_NAME);
            stack.remove(DataComponents.CUSTOM_NAME);
        }
    }
}
