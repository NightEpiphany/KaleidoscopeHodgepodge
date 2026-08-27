package com.moigferdsrte.kaleidoscopehodgepodge.client.screen;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.network.SelectLunchBoxIngredientPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public final class LunchBoxScreen extends AbstractContainerScreen<LunchBoxMenu> {
    private static final Identifier TEXTURE = KaleidoscopeHodgepodge.id(
            "textures/gui/container/lunch_box.png");
    private Button modeButton;

    public LunchBoxScreen(LunchBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) - 5;
        modeButton = addRenderableWidget(Button.builder(Component.literal(menu.mode() == PackingBagMode.PLACEMENT ? "P" : "S"), _ -> {
            if (minecraft.gameMode != null) {
                menu.setClientMode(menu.mode().next());
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 133, topPos + 61, 18, 18).build());
        updateModeButton();
    }

    @Override
    protected void slotClicked(@NonNull Slot slot, int slotId, int buttonNum, @NonNull ContainerInput containerInput) {
        if (slot != null && slot.index < LunchBoxMenu.BOX_SLOT_COUNT
                && menu.getCarried().isEmpty() && containerInput != ContainerInput.QUICK_CRAFT) {
            int selected = menu.hasProjectedItem(slot.index) ? slot.index : -1;
            menu.setClientSelectedSlot(selected);
            if (minecraft.player != null && minecraft.getConnection() != null
                    && ClientPlayNetworking.canSend(SelectLunchBoxIngredientPayload.TYPE)) {
                ClientPlayNetworking.send(new SelectLunchBoxIngredientPayload(
                        menu.containerId, selected, menu.hand()));
            }
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        int selected = menu.selectedSlot();
        if (selected >= 0) {
            int slotX = leftPos + 26 + (selected % 5) * 18;
            int slotY = topPos + 17 + (selected / 5) * 18;
            graphics.fill(RenderPipelines.GUI, slotX, slotY, slotX + 18, slotY + 1, 0xFFFFFFFF);
            graphics.fill(RenderPipelines.GUI, slotX, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);
            graphics.fill(RenderPipelines.GUI, slotX, slotY, slotX + 1, slotY + 18, 0xFFFFFFFF);
            graphics.fill(RenderPipelines.GUI, slotX + 17, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF);
        }
        graphics.item(menu.previewStack(), leftPos + 134, topPos + 35);
        updateModeButton();
    }

    private void updateModeButton() {
        if (modeButton != null) {
            PackingBagMode mode = menu.mode();
            modeButton.setMessage(Component.literal(menu.mode() == PackingBagMode.PLACEMENT ? "P" : "S"));
            modeButton.setTooltip(Tooltip.create(
                    Component.translatable("tooltip.kaleidoscope_hodgepodge.bag_mode",
                            Component.translatable(mode.translationKey()))));
        }
    }
}
