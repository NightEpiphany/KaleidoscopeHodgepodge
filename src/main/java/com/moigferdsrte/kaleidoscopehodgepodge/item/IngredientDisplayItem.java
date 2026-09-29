package com.moigferdsrte.kaleidoscopehodgepodge.item;

import net.minecraft.world.item.Item;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public class IngredientDisplayItem extends Item {
    public IngredientDisplayItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @NotNull TooltipContext context, @NotNull java.util.List<Component> builder, @NotNull TooltipFlag tooltipFlag) {
        if (!itemStack.has(KHDataComponents.INGREDIENT_DISPLAY_MODEL.get())
                || itemStack.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL.get(), "").isBlank()
                || Objects.requireNonNull(itemStack.get(KHDataComponents.INGREDIENT_DISPLAY_MODEL.get())).split("/").length != 3
        )
            super.appendHoverText(itemStack, context, builder, tooltipFlag);
        else builder.add(
                Component.literal(
                        MiscUtil.capitalize(
                                itemStack.getOrDefault(
                                        KHDataComponents.INGREDIENT_DISPLAY_MODEL.get(),
                                        "ingredient_display/nothing/empty")
                                        .split("/")[2]
                                        .replace('_', ' '))
                ).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
