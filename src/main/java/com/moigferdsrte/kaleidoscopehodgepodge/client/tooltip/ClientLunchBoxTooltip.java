package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public final class ClientLunchBoxTooltip implements ClientTooltipComponent {
    private static final int ROW_HEIGHT = 18;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final Component selectedLabel;
    private final Component totalLine;
    private final ItemStack selectedStack;

    public ClientLunchBoxTooltip(LunchBoxTooltip tooltip) {
        selectedStack = tooltip.selectedIngredientId()
                .map(IngredientModelService::createDisplay)
                .orElse(ItemStack.EMPTY);
        selectedLabel = Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.lunch_box_selected").append(" ");
        totalLine = Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.lunch_box_total", tooltip.totalCount());
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return selectedStack.isEmpty() ? ROW_HEIGHT : ROW_HEIGHT * 2;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        int selectedWidth = selectedStack.isEmpty() ? 0 : font.width(selectedLabel) + 16;
        return Math.max(selectedWidth, font.width(totalLine));
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height,
                             @NonNull GuiGraphicsExtractor graphics) {
        if (!selectedStack.isEmpty()) {
            graphics.text(font, selectedLabel, x, y + 4, TEXT_COLOR);
            graphics.fakeItem(selectedStack, x + font.width(selectedLabel), y + 1);
        }
        graphics.text(font, totalLine, x,
                y + (selectedStack.isEmpty() ? 4 : ROW_HEIGHT + 4), TEXT_COLOR);
    }
}
