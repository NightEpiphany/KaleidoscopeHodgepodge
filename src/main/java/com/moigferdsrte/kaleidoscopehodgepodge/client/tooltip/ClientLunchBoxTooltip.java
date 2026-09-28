package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ClientLunchBoxTooltip implements ClientTooltipComponent {
    private final ItemStack selected;
    private final Component selectedLabel = Component.translatable("tooltip.kaleidoscope_hodgepodge.lunch_box_selected");
    private final Component total;
    private final int percent;

    public ClientLunchBoxTooltip(LunchBoxTooltip tooltip) {
        selected = tooltip.selectedIngredientId().map(IngredientModelService::createDisplay).orElse(ItemStack.EMPTY);
        total = Component.translatable("tooltip.kaleidoscope_hodgepodge.lunch_box_total", tooltip.totalCount());
        percent = Math.round(tooltip.occupiedSlots() * 100.0F / 15.0F);
    }

    public int getHeight() { return (selected.isEmpty() ? 18 : 36) + 17; }
    public int getWidth(Font font) {
        return Math.max(96, Math.max(font.width(total), selected.isEmpty() ? 0 : font.width(selectedLabel) + 20));
    }
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (!selected.isEmpty()) {
            graphics.drawString(font, selectedLabel, x, y + 4, 0xFFFFFFFF);
            graphics.renderFakeItem(selected, x + font.width(selectedLabel) + 4, y + 1);
        }
        graphics.drawString(font, total, x, y + (selected.isEmpty() ? 4 : 22), 0xFFFFFFFF);
        int barX = x + (getWidth(font) - 96) / 2;
        int barY = y + (selected.isEmpty() ? 18 : 36) + 4;
        graphics.fill(barX, barY, barX + 96, barY + 13, 0xFF202020);
        graphics.fill(barX + 1, barY + 1, barX + 95, barY + 12, 0xFF555555);
        int fill = Math.round(94 * percent / 100.0F);
        int color = percent <= 33 ? 0xFF55AA55 : percent <= 66 ? 0xFFE0C040 : 0xFFD04040;
        if (fill > 0) graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 12, color);
        graphics.drawCenteredString(font, percent + "%", barX + 48, barY + 3, 0xFFFFFFFF);
    }
}
