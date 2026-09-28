package com.moigferdsrte.kaleidoscopehodgepodge;

import com.moigferdsrte.kaleidoscopehodgepodge.client.interaction.PackingBagRotationClientHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeRecipeBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.TeaTrayBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.*;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.FeastPlacementOutline;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeFeastBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.screen.LunchBoxScreen;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientHodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientIngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientFeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientLunchBoxTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.init.*;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class KaleidoscopeHodgepodgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PackingBagRotationClientHandler.register();
        ModelLoadingPlugin.register(context -> {
            var models = new ArrayList<ResourceLocation>(PackingIngredients.values().length + 40);
            for (PackingIngredients ingredient : PackingIngredientRegistry.all().values()) {
                models.add(KaleidoscopeHodgepodge.id("item/" + ingredient.getResourceLoc()));
            }
            for (var item : BuiltInRegistries.ITEM) {
                var cupModel = TeaTrayBlockEntityRenderer.cupModel(
                        BuiltInRegistries.ITEM.getKey(item));
                if (cupModel != null) models.add(cupModel);
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
        BlockRenderLayerMap.INSTANCE.putBlock(KHBlocks.HODGEPODGE_RECIPE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(KHBlocks.TEA_TRAY, RenderType.cutout());
        ColorProviderRegistry.ITEM.register(
                (stack, tint) -> tint == 0
                        ? stack.getOrDefault(DataComponents.DYED_COLOR,
                        new DyedItemColor(
                                LunchBoxItem.DEFAULT_COLOR, false)).rgb() | 0xFF000000
                        : -1, KHItems.LUNCH_BOX);
        ItemProperties.register(KHItems.LUNCH_BOX, KaleidoscopeHodgepodge.id("open"), (stack, level, entity, seed) -> {
            var player = Minecraft.getInstance().player;
            return player != null && player.containerMenu instanceof LunchBoxMenu menu
                    && player.getItemInHand(menu.hand()) == stack ? 1 : 0;
        });
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
        BlockEntityRenderers.register(KHBlockEntities.TEA_TRAY,
                TeaTrayBlockEntityRenderer::new);
        BlockEntityRenderers.register(KHBlockEntities.RECIPE,
               HodgepodgeRecipeBlockEntityRenderer::new);
        MenuScreens.register(KHMenus.LUNCH_BOX, LunchBoxScreen::new);
        FeastPlacementOutline.register();
        TooltipComponentCallback.EVENT.register(component -> {
            if (component instanceof LunchBoxTooltip tooltip)
                return new ClientLunchBoxTooltip(tooltip);
            if (component instanceof HodgepodgeRecipeTooltip tooltip)
                return new ClientHodgepodgeRecipeTooltip(tooltip);
            if (component instanceof IngredientTooltip tooltip) return new ClientIngredientTooltip(tooltip);
            if (component instanceof FeastIngredientsTooltip tooltip) return new ClientFeastIngredientsTooltip(tooltip);
            return null;
        });
    }
}
