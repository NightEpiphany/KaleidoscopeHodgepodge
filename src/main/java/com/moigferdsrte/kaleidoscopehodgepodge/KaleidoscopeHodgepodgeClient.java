package com.moigferdsrte.kaleidoscopehodgepodge;

import com.moigferdsrte.kaleidoscopehodgepodge.client.model.IngredientDisplayItemModel;
import com.moigferdsrte.kaleidoscopehodgepodge.client.model.WrappingBagItemModel;
import com.moigferdsrte.kaleidoscopehodgepodge.client.interaction.PackingBagRotationClientHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.CustomFeastSpecialRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.FeastPlacementOutline;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeFeastBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.screen.LunchBoxScreen;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientIngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientFeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHMenus;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;

@Environment(EnvType.CLIENT)
public class KaleidoscopeHodgepodgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemModels.ID_MAPPER.put(KaleidoscopeHodgepodge.id("ingredient_display"),
                IngredientDisplayItemModel.Unbaked.MAP_CODEC);
        ItemModels.ID_MAPPER.put(KaleidoscopeHodgepodge.id("wrapping_bag_gui"),
                WrappingBagItemModel.Unbaked.MAP_CODEC);
        SpecialModelRenderers.ID_MAPPER.put(KaleidoscopeHodgepodge.id("custom_feast"),
                CustomFeastSpecialRenderer.Unbaked.MAP_CODEC);
        SpecialModelRenderers.ID_MAPPER.put(KaleidoscopeHodgepodge.id("custom_feast_asymmetry"),
                CustomFeastSpecialRenderer.Unbaked.MAP_CODEC_ASYMMETRY);
        BlockEntityRenderers.register(KHBlockEntities.FEAST, HodgepodgeFeastBlockEntityRenderer::new);
        MenuScreens.register(KHMenus.LUNCH_BOX, LunchBoxScreen::new);
        FeastPlacementOutline.register();
        PackingBagRotationClientHandler.register();
        ClientTooltipComponentCallback.EVENT.register(component -> {
            if (component instanceof IngredientTooltip tooltip) return new ClientIngredientTooltip(tooltip);
            if (component instanceof FeastIngredientsTooltip tooltip) return new ClientFeastIngredientsTooltip(tooltip);
            return null;
        });
    }
}
