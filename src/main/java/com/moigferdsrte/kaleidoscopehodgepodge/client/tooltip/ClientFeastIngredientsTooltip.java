package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientFeastIngredientsTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 9;
    private static final int ICON_STRIDE = 18;
    private static final int LABEL_GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private final Component label = Component.translatable(
            "tooltip.kaleidoscope_hodgepodge.contained_ingredients_label");
    private final List<ItemStack> ingredientStacks;

    public ClientFeastIngredientsTooltip(FeastIngredientsTooltip tooltip) {
        ingredientStacks = tooltip.ingredientIds().stream().map(IngredientModelService::createDisplay).toList();
    }

    @Override
    public int getHeight() {
        return 9 + LABEL_GAP + rows() * ICON_STRIDE;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        int iconWidth = Math.min(COLUMNS, ingredientStacks.size()) * ICON_STRIDE;
        return Math.max(font.width(label), iconWidth);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics graphics) {
        graphics.drawString(font, label, x, y, TEXT_COLOR, true);
        int iconY = y + font.lineHeight + LABEL_GAP;
        for (int index = 0; index < ingredientStacks.size(); index++) {
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            graphics.renderItem(ingredientStacks.get(index), x + column * ICON_STRIDE, iconY + row * ICON_STRIDE);
        }
    }

    private int rows() {
        return (ingredientStacks.size() + COLUMNS - 1) / COLUMNS;
    }
}
