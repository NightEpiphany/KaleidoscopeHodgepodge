package com.moigferdsrte.kaleidoscopehodgepodge;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.*;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.FeastPlacementOutline;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeFeastBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.screen.LunchBoxScreen;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientIngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientFeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.init.*;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.interaction.PackingBagRotationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.item.ItemProperties;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class KaleidoscopeHodgepodgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                PackingBagRotationHandler.clientTick(client.options.keyAttack.isDown()));
        ModelLoadingPlugin.register(context -> {
            var models = new ArrayList<net.minecraft.resources.ResourceLocation>();
            for (PackingIngredients ingredient : PackingIngredients.values()) {
                boolean missingOptionalMod = ingredient.getSrcFoodIds().stream().anyMatch(id ->
                        id.getNamespace().equals("kaleidoscope_nether")
                                && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("kaleidoscope_nether")
                                || id.getNamespace().equals("kaleidoscope_end")
                                && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("kaleidoscope_end"));
                if (!missingOptionalMod) {
                    models.add(KaleidoscopeHodgepodge.id("item/" + ingredient.getResourceLoc()));
                }
            }
            models.add(KaleidoscopeHodgepodge.id("item/wrapping_bag"));
            models.add(KaleidoscopeHodgepodge.id("item/wooden_plate_empty"));
            models.add(KaleidoscopeHodgepodge.id("item/bamboo_display_tray_empty"));
            models.add(KaleidoscopeHodgepodge.id("item/porcelain_plate_empty"));
            models.add(KaleidoscopeHodgepodge.id("item/median_porcelain_plate_empty"));
            models.add(KaleidoscopeHodgepodge.id("item/large_porcelain_plate_empty"));
            models.add(KaleidoscopeHodgepodge.id("item/median_porcelain_plate_base"));
            models.add(KaleidoscopeHodgepodge.id("item/large_porcelain_plate_base"));
            models.add(KaleidoscopeHodgepodge.id("item/porcelain_soup_bowl_empty_with_soup"));
            models.add(KaleidoscopeHodgepodge.id("item/porcelain_soup_bowl_empty_without_soup"));
            models.add(KaleidoscopeHodgepodge.id("block/wooden_plate"));
            models.add(KaleidoscopeHodgepodge.id("block/bamboo_display_tray"));
            models.add(KaleidoscopeHodgepodge.id("block/porcelain_plate"));
            models.add(KaleidoscopeHodgepodge.id("block/porcelain_soup_bowl_with_soup"));
            models.add(KaleidoscopeHodgepodge.id("block/porcelain_soup_bowl_without_soup"));
            context.addModels(models);
        });
        BlockRenderLayerMap.INSTANCE.putBlock(KHBlocks.BAMBOO_DISPLAY_TRAY, RenderType.cutout());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.INGREDIENT_DISPLAY, new DefaultItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.WOODEN_PLATE, new PlateItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.PORCELAIN_PLATE, new PlateItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.BAMBOO_DISPLAY_TRAY, new PlateItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.MEDIAN_PORCELAIN_PLATE, new MedianPlateItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.LARGE_PORCELAIN_PLATE, new LargePlateItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(KHItems.PORCELAIN_SOUP_BOWL, new BowlItemRenderer());
        ItemProperties.register(KHItems.WRAPPING_BAG,
                KaleidoscopeHodgepodge.id("filled"),
                (stack, level, entity, seed) -> stack.has(KHDataComponents.PACKING_BAG_CONTENTS)
                        || stack.has(KHDataComponents.PACKING_BAG_INGREDIENT) ? 1.0F : 0.0F);
        BlockEntityRenderers.register(KHBlockEntities.FEAST, HodgepodgeFeastBlockEntityRenderer::new);
        MenuScreens.register(KHMenus.LUNCH_BOX, LunchBoxScreen::new);
        FeastPlacementOutline.register();
        TooltipComponentCallback.EVENT.register(component -> {
            if (component instanceof IngredientTooltip tooltip) return new ClientIngredientTooltip(tooltip);
            if (component instanceof FeastIngredientsTooltip tooltip) return new ClientFeastIngredientsTooltip(tooltip);
            return null;
        });
    }
}
