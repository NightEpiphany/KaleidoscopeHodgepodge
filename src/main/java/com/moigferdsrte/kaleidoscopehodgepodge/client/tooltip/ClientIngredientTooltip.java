package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public final class ClientIngredientTooltip implements ClientTooltipComponent {
    private static final int PREVIEW_SIZE = 18;
    private final ItemStack displayStack;

    public ClientIngredientTooltip(IngredientTooltip tooltip) {
        displayStack = IngredientModelService.createDisplay(tooltip.ingredientId());
    }

    private static boolean expanded() {
        return Minecraft.getInstance().hasShiftDown();
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return expanded() ? PREVIEW_SIZE : 0;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        return expanded() ? PREVIEW_SIZE : 0;
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height,
                             @NonNull GuiGraphicsExtractor graphics) {
        if (expanded()) {
            graphics.fakeItem(displayStack, x + 1, y + 1);
        }
    }
}
