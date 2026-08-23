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
        NeoForge.EVENT_BUS.addListener(KaleidoscopeHodgepodgeClient::onClientTick);
        FeastPlacementOutline.register();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        PackingBagRotationHandler.clientTick(Minecraft.getInstance().options.keyAttack.isDown());
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(KHItems.WRAPPING_BAG.get(),
                KaleidoscopeHodgepodge.id("filled"),
                (stack, level, entity, seed) -> stack.has(KHDataComponents.PACKING_BAG_CONTENTS.get())
                        || stack.has(KHDataComponents.PACKING_BAG_INGREDIENT.get()) ? 1.0F : 0.0F));
    }

    private static void registerModels(ModelEvent.RegisterAdditional event) {
        for (PackingIngredients ingredient : PackingIngredients.values()) {
            boolean missingOptionalMod = ingredient.getSrcFoodIds().stream().anyMatch(id ->
                    id.getNamespace().equals("kaleidoscope_nether") && !ModList.get().isLoaded("kaleidoscope_nether")
                            || id.getNamespace().equals("kaleidoscope_end") && !ModList.get().isLoaded("kaleidoscope_end"));
            if (!missingOptionalMod) registerModel(event, "item/" + ingredient.getResourceLoc());
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
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(KHMenus.LUNCH_BOX.get(), LunchBoxScreen::new);
    }

    private static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(IngredientTooltip.class, ClientIngredientTooltip::new);
        event.register(FeastIngredientsTooltip.class, ClientFeastIngredientsTooltip::new);
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
