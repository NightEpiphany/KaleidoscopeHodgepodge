package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class ClientHodgepodgeRecipeTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 8;
    private static final int STRIDE = 18;
    private static final int HEADER = 35;
    private static final int AVATAR_SIZE = 16;
    private final Component ownerLabel;
    private final Optional<Component> dishNameLine;
    private final String recordedOwnerName;
    private final String ownerId;
    private final Supplier<PlayerSkinRenderCache.RenderInfo> ownerSkin;
    private final ItemStack preview;
    private final List<ItemStack> ingredients;

    public ClientHodgepodgeRecipeTooltip(HodgepodgeRecipeTooltip data) {
        dishNameLine = data.dishName().map(name -> Component.translatable(
                "tooltip.kaleidoscope_hodgepodge.dish_name", name));
        preview = data.preview().copy();
        ingredients = data.placements().stream()
                .map(placement -> IngredientModelService.createDisplay(placement.id())).toList();
        Minecraft minecraft = Minecraft.getInstance();
        GameProfile author = data.ownerProfile().orElseGet(() -> {
            var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(data.owner());
            return info == null ? null : info.getProfile();
        });
        recordedOwnerName = author == null ? "" : author.name();
        ownerId = data.owner().toString();
        ResolvableProfile profile = author != null && author.properties().containsKey("textures")
                ? ResolvableProfile.createResolved(author) : ResolvableProfile.createUnresolved(data.owner());
        PlayerSkinRenderCache skinCache = minecraft.playerSkinRenderCache();
        var skinLookup = skinCache.lookup(profile).exceptionally(failure -> {
            KaleidoscopeHodgepodge.LOGGER.warn("Error loading Skin", failure);
            return Optional.empty();
        });
        PlayerSkinRenderCache.RenderInfo fallback = skinCache.new RenderInfo(profile.partialProfile(),
                DefaultPlayerSkin.get(profile.partialProfile()), profile.skinPatch());
        ownerSkin = () -> skinLookup.getNow(Optional.empty()).orElse(fallback);
        ownerLabel = Component.translatable("tooltip.kaleidoscope_hodgepodge.recipe_owner_label");
    }

    private Component ownerName() {
        if (!recordedOwnerName.isBlank()) return Component.literal(recordedOwnerName);
        String resolvedName = ownerSkin.get().gameProfile().name();
        return Component.literal(resolvedName.isBlank() ? ownerId : resolvedName);
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return nameHeight(font) + HEADER + Math.max(1, (ingredients.size() + COLUMNS - 1) / COLUMNS) * STRIDE;
    }

    private int nameHeight(Font font) {
        return dishNameLine.isPresent() ? font.lineHeight + 6 : 0;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        int ownerWidth = font.width(ownerLabel) + 4 + font.width(ownerName()) + 4 + AVATAR_SIZE;
        return Math.max(dishNameLine.map(font::width).orElse(0), Math.max(180, Math.max(ownerWidth,
                Math.min(COLUMNS, Math.max(1, ingredients.size())) * STRIDE)));
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height,
                             @NonNull GuiGraphicsExtractor graphics) {
        if (dishNameLine.isPresent()) {
            graphics.text(font, dishNameLine.get().plainCopy().withStyle(ChatFormatting.GOLD), x, y + 2, 0xFFFFFFFF);
            y += nameHeight(font);
        }
        graphics.text(font, ownerLabel, x, y + 2, 0xFFFFFFFF);
        int nameX = x + font.width(ownerLabel) + 4;
        Component ownerName = ownerName();
        MutableComponent modelPreview = Component.translatable("tooltip.kaleidoscope_hodgepodge.model_preview");
        graphics.text(font, ownerName, nameX, y + 2, 0xFFFFFFFF);
        PlayerFaceExtractor.extractRenderState(graphics, ownerSkin.get().playerSkin(),
                nameX + font.width(ownerName) + 4, y - 2, AVATAR_SIZE);
        graphics.text(font, modelPreview, x, y + 16, 0xFFFFFFFF);
        graphics.fakeItem(preview, x + font.width(modelPreview) + 4, y + 16);
        int iconsY = y + HEADER;
        for (int index = 0; index < ingredients.size(); index++) {
            graphics.fakeItem(ingredients.get(index), x + (index % COLUMNS) * STRIDE,
                    iconsY + (index / COLUMNS) * STRIDE);
        }
    }
}
