package com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip;

import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class ClientHodgepodgeRecipeTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 8;
    private static final int STRIDE = 18;
    private static final int HEADER = 48;
    // DefaultPlayerSkin index 6 is entity/player/slim/steve.
    private static final UUID FALLBACK_SKIN_ID = new UUID(0L, 6L);
    private final Component ownerLabel;
    private final Component ownerName;
    private final ItemStack preview;
    private final ItemStack avatar;
    private final List<ItemStack> ingredients;

    public ClientHodgepodgeRecipeTooltip(HodgepodgeRecipeTooltip data) {
        preview = data.preview().copy();
        ingredients = data.placements().stream().map(p -> IngredientModelService.createDisplay(p.id())).toList();
        Minecraft minecraft = Minecraft.getInstance();
        var info = minecraft.getConnection() == null || data.owner() == null
                ? null : minecraft.getConnection().getPlayerInfo(data.owner());
        boolean hasProfile = info != null && info.getProfile() != null
                && info.getProfile().name() != null && !info.getProfile().name().isBlank();
        ResolvableProfile profile = hasProfile ? ResolvableProfile.createResolved(info.getProfile())
                : ResolvableProfile.createUnresolved(FALLBACK_SKIN_ID);
        PlayerSkinRenderCache.RenderInfo skin = minecraft.playerSkinRenderCache().getOrDefault(profile);
        ResolvableProfile avatarProfile = skin != null && skin.gameProfile() != null
                ? ResolvableProfile.createResolved(skin.gameProfile()) : profile;
        ownerLabel = Component.translatable("tooltip.kaleidoscope_hodgepodge.recipe_owner_label");
        ownerName = Component.literal(hasProfile ? info.getProfile().name() : "Unkown");
        avatar = Items.PLAYER_HEAD.getDefaultInstance();
        avatar.set(DataComponents.PROFILE, avatarProfile);
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return HEADER + Math.max(1, (ingredients.size() + COLUMNS - 1) / COLUMNS) * STRIDE;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        int ownerWidth = font.width(ownerLabel) + 4 + font.width(ownerName) + 20;
        return Math.max(180, Math.max(ownerWidth,
                Math.min(COLUMNS, Math.max(1, ingredients.size())) * STRIDE));
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int width, int height,
                             @NonNull GuiGraphicsExtractor graphics) {
        graphics.text(font, ownerLabel, x, y + 2, 0xFFFFFFFF);
        int nameX = x + font.width(ownerLabel) + 4;
        graphics.text(font, ownerName, nameX, y + 2, 0xFFFFFFFF);
        graphics.fakeItem(avatar, nameX + font.width(ownerName) + 4, y - 2);
        graphics.fakeItem(preview, x, y + 16);
        int iconsY = y + HEADER;
        for (int i = 0; i < ingredients.size(); i++) {
            graphics.fakeItem(ingredients.get(i), x + (i % COLUMNS) * STRIDE,
                    iconsY + (i / COLUMNS) * STRIDE);
        }
    }
}
