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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientIngredientTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 9;
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_STRIDE = 18;
    private static final int ICON_GAP = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private final Component ingredientLabel = Component.translatable(
            "tooltip.kaleidoscope_hodgepodge.contained_ingredients_label");
    private final Component sourceDishLabel = Component.translatable(
            "tooltip.kaleidoscope_hodgepodge.source_dish_label");
    private final List<ItemStack> ingredientStacks;
    private final List<ItemStack> sourceDishStacks;

    public ClientIngredientTooltip(IngredientTooltip tooltip) {
        ingredientStacks = tooltip.ingredientIds().stream().map(IngredientModelService::createDisplay).toList();
        sourceDishStacks = tooltip.sourceDishIds().stream()
                .map(id -> BuiltInRegistries.ITEM.getOptional(id)
                        .map(Item::getDefaultInstance)
                        .orElse(ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return ROW_HEIGHT * (1 + sourceRows());
    }

    @Override
    public int getWidth(@NonNull Font font) {
        int ingredientWidth = font.width(ingredientLabel) + ICON_GAP
                + Math.max(16, ingredientStacks.size() * ICON_STRIDE);
        int sourceWidth = font.width(sourceDishLabel) + ICON_GAP
                + Math.max(16, Math.min(COLUMNS, sourceDishStacks.size()) * ICON_STRIDE);
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
        int sourceX = x + font.width(sourceDishLabel) + ICON_GAP;
        int sourceY = y + ROW_HEIGHT;
        graphics.text(font, sourceDishLabel, x, sourceY + 4, TEXT_COLOR);
        for (int index = 0; index < sourceDishStacks.size(); index++) {
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            graphics.fakeItem(sourceDishStacks.get(index), sourceX + column * ICON_STRIDE,
                    sourceY + row * ROW_HEIGHT + 1);
        }
    }

    private int sourceRows() {
        return Math.max(1, (sourceDishStacks.size() + COLUMNS - 1) / COLUMNS);
    }
}
