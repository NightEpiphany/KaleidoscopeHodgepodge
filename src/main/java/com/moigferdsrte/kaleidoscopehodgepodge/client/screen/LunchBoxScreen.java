package com.moigferdsrte.kaleidoscopehodgepodge.client.screen;

import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.network.SelectLunchBoxIngredientPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public final class LunchBoxScreen extends AbstractContainerScreen<LunchBoxMenu> {
    private static final ResourceLocation TEXTURE = KaleidoscopeHodgepodge.id(
            "textures/gui/container/lunch_box.png");
    private Button modeButton;

    public LunchBoxScreen(LunchBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        modeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft.gameMode != null) {
                menu.setClientMode(menu.mode().next());
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 133, topPos + 61, 18, 18).build());
        updateModeButton();
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType clickType) {
        if (slot != null && slotId >= 0 && slotId < LunchBoxMenu.BOX_SLOT_COUNT
                && menu.getCarried().isEmpty() && clickType != ClickType.QUICK_CRAFT) {
            int selected = menu.hasProjectedItem(slotId) ? slotId : -1;
            menu.setClientSelectedSlot(selected);
            if (ClientPlayNetworking.canSend(SelectLunchBoxIngredientPayload.TYPE)) {
                ClientPlayNetworking.send(new SelectLunchBoxIngredientPayload(menu.containerId, selected, menu.hand()));
            }
            return;
        }
        super.slotClicked(slot, slotId, button, clickType);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int selected = menu.selectedSlot();
        if (selected >= 0) {
            int x = leftPos + 25 + selected % 5 * 18;
            int y = topPos + 16 + selected / 5 * 18;
            graphics.renderOutline(x, y, 18, 18, 0xFFFFFFFF);
        }
        graphics.renderItem(menu.previewStack(), leftPos + 134, topPos + 35);
        updateModeButton();
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void updateModeButton() {
        modeButton.setMessage(Component.literal(menu.mode() == PackingBagMode.PLACEMENT ? "P" : "S"));
        modeButton.setTooltip(Tooltip.create(Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.bag_mode", Component.translatable(menu.mode().translationKey()))));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
