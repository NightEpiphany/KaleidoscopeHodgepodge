package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class ClientHodgepodgeRecipeTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 8;
    private static final int STRIDE = 18;
    private static final int HEADER = 35;
    private static final int AVATAR_SIZE = 16;
    private final ItemStack preview;
    private final List<ItemStack> ingredients;
    private final Component authorLabel = Component.translatable("tooltip.kaleidoscope_hodgepodge.recipe_owner_label");
    private final Component author;
    private final Optional<Component> dishName;
    private final Supplier<PlayerSkin> skin;

    public ClientHodgepodgeRecipeTooltip(HodgepodgeRecipeTooltip tooltip) {
        preview = tooltip.preview();
        ingredients = tooltip.placements().stream()
                .map(placement -> IngredientModelService.createDisplay(placement.id())).toList();
        dishName = tooltip.dishName().map(name -> Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.dish_name", name).withStyle(ChatFormatting.GOLD));
        Minecraft minecraft = Minecraft.getInstance();
        GameProfile profile = tooltip.ownerProfile().orElseGet(() -> {
            var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(tooltip.owner());
            return info == null ? null : info.getProfile();
        });
        String name = profile == null ? null : profile.getName();
        author = name == null || name.isBlank() || looksLikeUuid(name)
                ? Component.translatable("tooltip.kaleidoscope_hodgepodge.unknown_owner") : Component.literal(name);
        skin = profile == null ? () -> DefaultPlayerSkin.get(tooltip.owner())
                : minecraft.getSkinManager().lookupInsecure(profile);
    }

    private static boolean looksLikeUuid(String name) {
        try {
            java.util.UUID.fromString(name);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public int getHeight() {
        return (dishName.isPresent() ? Minecraft.getInstance().font.lineHeight + 6 : 0)
                + HEADER + Math.max(1, (ingredients.size() + COLUMNS - 1) / COLUMNS) * STRIDE;
    }

    public int getWidth(Font font) {
        int authorWidth = font.width(authorLabel) + font.width(author) + AVATAR_SIZE + 8;
        return Math.max(dishName.map(font::width).orElse(0),
                Math.max(180, Math.max(authorWidth, Math.min(COLUMNS, ingredients.size()) * STRIDE)));
    }

    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (dishName.isPresent()) {
            graphics.drawString(font, dishName.get(), x, y + 2, 0xFFFFFFFF);
            y += font.lineHeight + 6;
        }
        graphics.drawString(font, authorLabel, x, y + 2, 0xFFFFFFFF);
        int nameX = x + font.width(authorLabel) + 4;
        graphics.drawString(font, author, nameX, y + 2, 0xFFFFFFFF);
        PlayerFaceRenderer.draw(graphics, skin.get(), nameX + font.width(author) + 4, y - 2, AVATAR_SIZE);
        Component modelLabel = Component.translatable("tooltip.kaleidoscope_hodgepodge.model_preview");
        graphics.drawString(font, modelLabel, x, y + 16, 0xFFFFFFFF);
        graphics.renderFakeItem(preview, x + font.width(modelLabel) + 4, y + 16);
        for (int index = 0; index < ingredients.size(); index++) {
            graphics.renderFakeItem(ingredients.get(index), x + index % COLUMNS * STRIDE,
                    y + HEADER + index / COLUMNS * STRIDE);
        }
    }
}
