package com.moigferdsrte.kaleidoscopehodgepodge.smoketest;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewPositionAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.client.animation.IngredientPreviewRotationAnimation;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;

/** Runs only with -PclientSmoke; captures actual game rendering, then exits. */
public final class ClientSmoke implements ClientModInitializer {
    private int ticks;
    private boolean started;

    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(context ->
                context.addModels(java.util.List.of(KaleidoscopeHodgepodge.id("item/lunch_box_open"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (started || ++ticks < 100 || client.getOverlay() != null) return;
            started = true;
            checkModels(client);
            checkAnimations();
            checkCreativeTabs(client);
            client.options.guiScale().set(2);
            client.resizeDisplay();
            client.setScreen(new SmokeScreen());
        });
    }

    private static void checkModels(Minecraft client) {
        var manager = (FabricBakedModelManager) client.getModelManager();
        int boxColor = net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM
                .get(KHItems.LUNCH_BOX).getColor(new ItemStack(KHItems.LUNCH_BOX), 0);
        require((boxColor >>> 24) == 255, "Lunchbox tint must have an opaque alpha channel");
        var openBox = manager.getModel(KaleidoscopeHodgepodge.id("item/lunch_box_open"));
        require(openBox != null && openBox != client.getModelManager().getMissingModel(), "Missing open lunchbox model");
        for (var ingredient : PackingIngredientRegistry.all().values()) {
            BakedModel model = manager.getModel(KaleidoscopeHodgepodge.id("item/" + ingredient.getResourceLoc()));
            require(model != null && model != client.getModelManager().getMissingModel(), "Missing model: " + ingredient.getId());
            int vertices = 0;
            for (int face = 0; face <= 6; face++) {
                for (var quad : model.getQuads(null, face == 6 ? null : Direction.from3DDataValue(face),
                        RandomSource.create(42))) {
                    require(!quad.getSprite().contents().name().getPath().equals("missingno"),
                            "Missing texture: " + ingredient.getId());
                    vertices += 4;
                }
            }
            require(vertices > 0, "Empty model: " + ingredient.getId());
        }
        KaleidoscopeHodgepodge.LOGGER.info("CLIENT_SMOKE: {} ingredient models and textures verified",
                PackingIngredientRegistry.all().size());
    }

    private static void checkAnimations() {
        var id = KaleidoscopeHodgepodge.id("mutton");
        var position = new IngredientPreviewPositionAnimation();
        position.beginFrame();
        position.update(BlockPos.ZERO, id, 0, 0, 0, 0, 0L);
        position.beginFrame();
        position.update(BlockPos.ZERO, id, 0, 8, 4, 12, 0L);
        position.sample(30_000_000L);
        require(position.x() == 4 && position.y() == 2 && position.z() == 6, "Position interpolation failed");
        position.beginFrame();
        position.update(BlockPos.ZERO, id, 0, 16, 8, 24, 30_000_000L);
        position.sample(30_000_000L);
        require(position.x() == 4, "Retargeting position was discontinuous");
        var rotation = new IngredientPreviewRotationAnimation();
        rotation.beginFrame();
        rotation.update(BlockPos.ZERO, id, 0, 3, 0L);
        rotation.beginFrame();
        rotation.update(BlockPos.ZERO, id, 0, 0, 0L);
        require(rotation.sample(120_000_000L) == -315, "Wrapped rotation was not clockwise");
        rotation.beginFrame();
        rotation.update(BlockPos.ZERO, id, 0, 1, 120_000_000L);
        require(rotation.sample(120_000_000L) == -315, "Retargeting rotation was discontinuous");
        require(rotation.sample(360_000_000L) == -450, "Rotation did not finish at target");
        KaleidoscopeHodgepodge.LOGGER.info("CLIENT_SMOKE: movement and rotation continuity verified");
    }

    private static void checkCreativeTabs(Minecraft client) {
        FoodBiteRegistry.FOOD_DATA_MAP.keySet().forEach(id -> {
            var item = BuiltInRegistries.ITEM.get(id);
            var defaultInstance = item.getDefaultInstance();
            var constructed = new ItemStack(item);
            KaleidoscopeHodgepodge.LOGGER.info(
                    "CLIENT_SMOKE: food {} -> {} ({}) defaultCount={} constructedCount={} maxStackSize={}",
                    id, BuiltInRegistries.ITEM.getKey(item), item.getClass().getName(),
                    defaultInstance.getCount(), constructed.getCount(), defaultInstance.getMaxStackSize());
        });
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
                FeatureFlags.DEFAULT_FLAGS, false,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        KaleidoscopeHodgepodge.LOGGER.info("CLIENT_SMOKE: creative tab contents rebuilt successfully");
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
