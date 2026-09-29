package com.moigferdsrte.kaleidoscopehodgepodge;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.FeastPlacementOutline;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeFeastBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.ClientItemExtensions;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.BowlItemRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.DefaultItemRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.LargePlateItemRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.MedianPlateItemRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.PlateItemRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.screen.LunchBoxScreen;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientFeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientIngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHMenus;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.interaction.PackingBagRotationHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.client.interaction.PackingBagRotationClientHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.TeaTrayBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.HodgepodgeRecipeBlockEntityRenderer;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientHodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientLunchBoxTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public final class KaleidoscopeHodgepodgeClient {
    public static void register(IEventBus modBus) {
        modBus.addListener(KaleidoscopeHodgepodgeClient::onClientSetup);
        modBus.addListener(KaleidoscopeHodgepodgeClient::registerModels);
        modBus.addListener(KaleidoscopeHodgepodgeClient::registerRenderers);
        modBus.addListener(KaleidoscopeHodgepodgeClient::registerScreens);
        modBus.addListener(KaleidoscopeHodgepodgeClient::registerTooltips);
        modBus.addListener(KaleidoscopeHodgepodgeClient::registerClientExtensions);
        modBus.addListener(KaleidoscopeHodgepodgeClient::registerColors);
        PackingBagRotationClientHandler.register();
        FeastPlacementOutline.register();
    }

    private static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? stack.getOrDefault(DataComponents.DYED_COLOR,
                new DyedItemColor(LunchBoxItem.DEFAULT_COLOR, false)).rgb() | 0xFF000000 : -1,
                KHItems.LUNCH_BOX.get());
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(KHBlocks.BAMBOO_DISPLAY_TRAY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(KHBlocks.HODGEPODGE_RECIPE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(KHBlocks.TEA_TRAY.get(), RenderType.cutout());
            ItemProperties.register(KHItems.LUNCH_BOX.get(), KaleidoscopeHodgepodge.id("open"),
                    (stack, level, entity, seed) -> {
                        var player = Minecraft.getInstance().player;
                        return player != null && player.containerMenu instanceof LunchBoxMenu menu
                                && player.getItemInHand(menu.hand()) == stack ? 1 : 0;
                    });
            ItemProperties.register(KHItems.WRAPPING_BAG.get(),
                KaleidoscopeHodgepodge.id("filled"),
                (stack, level, entity, seed) -> stack.has(KHDataComponents.PACKING_BAG_CONTENTS.get())
                        || stack.has(KHDataComponents.PACKING_BAG_INGREDIENT.get()) ? 1.0F : 0.0F);
        });
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        for (PackingIngredients ingredient : PackingIngredientRegistry.all().values()) {
            registerModel(event, "item/" + ingredient.getResourceLoc());
        }
        for (var item : BuiltInRegistries.ITEM) {
            var model = TeaTrayBlockEntityRenderer.cupModel(BuiltInRegistries.ITEM.getKey(item));
            if (model != null) event.register(ModelResourceLocation.standalone(model));
        }
        registerModel(event, "item/wrapping_bag");
        registerModel(event, "item/wooden_plate_empty");
        registerModel(event, "item/bamboo_display_tray_empty");
        registerModel(event, "item/porcelain_plate_empty");
        registerModel(event, "item/median_porcelain_plate_empty");
        registerModel(event, "item/large_porcelain_plate_empty");
        registerModel(event, "item/median_porcelain_plate_base");
        registerModel(event, "item/large_porcelain_plate_base");
        registerModel(event, "item/porcelain_soup_bowl_empty_with_soup");
        registerModel(event, "item/porcelain_soup_bowl_empty_without_soup");
        registerModel(event, "block/wooden_plate");
        registerModel(event, "block/bamboo_display_tray");
        registerModel(event, "block/porcelain_plate");
        registerModel(event, "block/porcelain_soup_bowl_with_soup");
        registerModel(event, "block/porcelain_soup_bowl_without_soup");
    }

    private static void registerModel(ModelEvent.RegisterAdditional event, String path) {
        event.register(ModelResourceLocation.standalone(KaleidoscopeHodgepodge.id(path)));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(KHBlockEntities.FEAST.get(), HodgepodgeFeastBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(KHBlockEntities.TEA_TRAY.get(), TeaTrayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(KHBlockEntities.RECIPE.get(), HodgepodgeRecipeBlockEntityRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(KHMenus.LUNCH_BOX.get(), LunchBoxScreen::new);
    }

    private static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(IngredientTooltip.class, ClientIngredientTooltip::new);
        event.register(FeastIngredientsTooltip.class, ClientFeastIngredientsTooltip::new);
        event.register(HodgepodgeRecipeTooltip.class, ClientHodgepodgeRecipeTooltip::new);
        event.register(LunchBoxTooltip.class, ClientLunchBoxTooltip::new);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(ClientItemExtensions.renderer(DefaultItemRenderer::new), KHItems.INGREDIENT_DISPLAY.get());
        event.registerItem(ClientItemExtensions.renderer(PlateItemRenderer::new),
                KHItems.WOODEN_PLATE.get(), KHItems.PORCELAIN_PLATE.get(), KHItems.BAMBOO_DISPLAY_TRAY.get());
        event.registerItem(ClientItemExtensions.renderer(MedianPlateItemRenderer::new),
                KHItems.MEDIAN_PORCELAIN_PLATE.get());
        event.registerItem(ClientItemExtensions.renderer(LargePlateItemRenderer::new),
                KHItems.LARGE_PORCELAIN_PLATE.get());
        event.registerItem(ClientItemExtensions.renderer(BowlItemRenderer::new),
                KHItems.PORCELAIN_SOUP_BOWL.get());
    }

    private KaleidoscopeHodgepodgeClient() {}
}
