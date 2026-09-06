package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.github.ysbbbbbb.kaleidoscopecookery.item.ModelDisplayItem;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Consumer;

public class IngredientDisplayItem extends ModelDisplayItem {
    public IngredientDisplayItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(@NonNull ItemStack itemStack, @NonNull TooltipContext context, @NonNull TooltipDisplay display, @NonNull Consumer<Component> builder, @NonNull TooltipFlag tooltipFlag) {
        if (!itemStack.has(KHDataComponents.INGREDIENT_DISPLAY_MODEL)
                || itemStack.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL, "").isBlank()
                || Objects.requireNonNull(itemStack.get(KHDataComponents.INGREDIENT_DISPLAY_MODEL)).split("/").length != 3
        )
            super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        else builder.accept(
                Component.literal(
                        MiscUtil.capitalize(
                                itemStack.getOrDefault(
                                        KHDataComponents.INGREDIENT_DISPLAY_MODEL,
                                        "ingredient_display/nothing/empty")
                                        .split("/")[2]
                                        .replace('_', ' '))
                ).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
