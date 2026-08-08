package com.moigferdsrte.kaleidoscopehodgepodge;

import com.moigferdsrte.kaleidoscopehodgepodge.client.model.IngredientDisplayItemModel;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.CustomFeastSpecialRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeFeastBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientIngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
        SpecialModelRenderers.ID_MAPPER.put(KaleidoscopeHodgepodge.id("custom_feast"),
                CustomFeastSpecialRenderer.Unbaked.MAP_CODEC);
        BlockEntityRenderers.register(KHBlockEntities.FEAST, HodgepodgeFeastBlockEntityRenderer::new);
        ClientTooltipComponentCallback.EVENT.register(component -> component instanceof IngredientTooltip tooltip
                ? new ClientIngredientTooltip(tooltip) : null);
    }
}
