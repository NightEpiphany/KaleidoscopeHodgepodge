package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientIngredientTooltip implements ClientTooltipComponent {
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_STRIDE = 18;
    private static final int ICON_GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private final Component ingredientLabel = Component.translatable(
            "tooltip.kaleidoscope_hodgepodge.contained_ingredients_label");
    private final Component sourceDishLabel = Component.translatable(
            "tooltip.kaleidoscope_hodgepodge.source_dish_label");
    private final List<ItemStack> ingredientStacks;
    private final ItemStack sourceDishStack;

    public ClientIngredientTooltip(IngredientTooltip tooltip) {
        ingredientStacks = tooltip.ingredientIds().stream().map(IngredientModelService::createDisplay).toList();
        sourceDishStack = BuiltInRegistries.ITEM.getOptional(tooltip.sourceDishId())
                .map(item -> item.getDefaultInstance())
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return ROW_HEIGHT * 2;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        int ingredientWidth = font.width(ingredientLabel) + ICON_GAP
                + Math.max(16, ingredientStacks.size() * ICON_STRIDE);
        int sourceWidth = font.width(sourceDishLabel) + ICON_GAP + 16;
        return Math.max(ingredientWidth, sourceWidth);
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height,
                             @NonNull GuiGraphicsExtractor graphics) {
        graphics.text(font, ingredientLabel, x, y + 4, TEXT_COLOR);
        int iconX = x + font.width(ingredientLabel) + ICON_GAP;
        for (int index = 0; index < ingredientStacks.size(); index++) {
            graphics.fakeItem(ingredientStacks.get(index), iconX + index * ICON_STRIDE, y + 1);
        }
        drawSingleIconRow(graphics, font, sourceDishLabel, sourceDishStack, x, y + ROW_HEIGHT);
    }

    private static void drawSingleIconRow(GuiGraphicsExtractor graphics, Font font, Component label,
                                          ItemStack stack, int x, int y) {
        graphics.text(font, label, x, y + 4, TEXT_COLOR);
        graphics.fakeItem(stack, x + font.width(label) + ICON_GAP, y + 1);
    }
}
