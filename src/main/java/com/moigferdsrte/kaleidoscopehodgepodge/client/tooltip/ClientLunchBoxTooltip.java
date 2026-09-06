package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class ClientLunchBoxTooltip implements ClientTooltipComponent {
    private static final int ROW_HEIGHT = 18;
    private static final int PROGRESS_WIDTH = 96;
    private static final int PROGRESS_HEIGHT = 13;
    private static final int PROGRESS_FILL_WIDTH = 94;
    private static final int PROGRESS_MARGIN_Y = 4;
    private static final int PROGRESS_BORDER_COLOR = 0xFF202020;
    private static final int PROGRESS_BACKGROUND_COLOR = 0xFF555555;
    private static final int PROGRESS_GREEN_COLOR = 0xFF55AA55;
    private static final int PROGRESS_YELLOW_COLOR = 0xFFE0C040;
    private static final int PROGRESS_RED_COLOR = 0xFFD04040;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final Component selectedLabel;
    private final Component totalLine;
    private final ItemStack selectedStack;
    private final int occupiedSlots;
    private final int percent;

    public ClientLunchBoxTooltip(LunchBoxTooltip tooltip) {
        selectedStack = tooltip.selectedIngredientId() == null ? ItemStack.EMPTY :
                tooltip.selectedIngredientId()
                .map(IngredientModelService::createDisplay)
                .orElse(ItemStack.EMPTY);
        selectedLabel = Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.lunch_box_selected").append(" ");
        totalLine = Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.lunch_box_total", tooltip.totalCount());
        occupiedSlots = Math.clamp(tooltip.occupiedSlots(), 0, 15);
        percent = Math.round(occupiedSlots * 100.0F / 15.0F);
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return (selectedStack.isEmpty() ? ROW_HEIGHT : ROW_HEIGHT * 2)
                + PROGRESS_MARGIN_Y + PROGRESS_HEIGHT;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        int selectedWidth = selectedStack.isEmpty() ? 0 : font.width(selectedLabel) + 16;
        return Math.max(PROGRESS_WIDTH, Math.max(selectedWidth, font.width(totalLine)));
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height,
                             @NonNull GuiGraphicsExtractor graphics) {
        if (!selectedStack.isEmpty()) {
            graphics.text(font, selectedLabel, x, y + 4, TEXT_COLOR);
            graphics.fakeItem(selectedStack, x + font.width(selectedLabel), y + 1);
        }
        int totalY = y + (selectedStack.isEmpty() ? 4 : ROW_HEIGHT + 4);
        graphics.text(font, totalLine, x, totalY, TEXT_COLOR);

        int barY = y + (selectedStack.isEmpty() ? ROW_HEIGHT : ROW_HEIGHT * 2) + PROGRESS_MARGIN_Y;
        int barX = x + (width - PROGRESS_WIDTH) / 2;
        graphics.fill(RenderPipelines.GUI, barX, barY,
                barX + PROGRESS_WIDTH, barY + PROGRESS_HEIGHT, PROGRESS_BORDER_COLOR);
        graphics.fill(RenderPipelines.GUI, barX + 1, barY + 1,
                barX + PROGRESS_WIDTH - 1, barY + PROGRESS_HEIGHT - 1, PROGRESS_BACKGROUND_COLOR);
        int fillWidth = Math.round(PROGRESS_FILL_WIDTH * percent / 100.0F);
        if (fillWidth > 0) {
            graphics.fill(RenderPipelines.GUI, barX + 1, barY + 1,
                    barX + 1 + fillWidth, barY + PROGRESS_HEIGHT - 1, progressColor());
        }
        graphics.centeredText(font, Component.literal(percent + "%"),
                barX + PROGRESS_WIDTH / 2, barY + 3, TEXT_COLOR);
    }

    private int progressColor() {
        if (percent <= 33) return PROGRESS_GREEN_COLOR;
        if (percent <= 66) return PROGRESS_YELLOW_COLOR;
        return PROGRESS_RED_COLOR;
    }
}
