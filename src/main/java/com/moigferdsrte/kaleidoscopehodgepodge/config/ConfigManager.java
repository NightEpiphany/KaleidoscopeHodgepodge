package com.moigferdsrte.kaleidoscopehodgepodge.config;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.neoforged.fml.config.ModConfig;
import org.jetbrains.annotations.Contract;

public final class ConfigManager {
    private static final String COMMON_FILE = KaleidoscopeHodgepodge.MOD_ID + "-common.toml";
    private static final String CLIENT_FILE = KaleidoscopeHodgepodge.MOD_ID + "-client.toml";
    private static boolean started;

    @Contract(pure = true)
    public static synchronized void start() {
        if (started) return;
        started = true;
        ModConfigEvents.loading(KaleidoscopeHodgepodge.MOD_ID).register(ConfigManager::reload);
        ModConfigEvents.reloading(KaleidoscopeHodgepodge.MOD_ID).register(ConfigManager::reload);
        ConfigRegistry.INSTANCE.register(KaleidoscopeHodgepodge.MOD_ID, ModConfig.Type.COMMON,
                GeneralConfig.COMMON_SPEC, COMMON_FILE);
        ConfigRegistry.INSTANCE.register(KaleidoscopeHodgepodge.MOD_ID, ModConfig.Type.CLIENT,
                GeneralConfig.CLIENT_SPEC, CLIENT_FILE);
        GeneralConfig.reloadCommon();
        GeneralConfig.reloadClient();
        KaleidoscopeHodgepodge.LOGGER.info("Loaded Forge Config API Port configuration: {}", describe());
    }

    private static void reload(ModConfig config) {
        if (config.getType() == ModConfig.Type.CLIENT) GeneralConfig.reloadClient();
        else if (config.getType() == ModConfig.Type.COMMON) GeneralConfig.reloadCommon();
        KaleidoscopeHodgepodge.LOGGER.info("Reloaded {}: {}", config.getFileName(), describe());
    }

    public static String describe() {
        GeneralConfig.Snapshot value = GeneralConfig.snapshot();
        return "woodenPlateCapacity=" + value.woodenPlateCapacity()
                + ", porcelainCapacity=" + value.porcelainCapacity()
                + ", soupCapacity=" + value.soupCapacity()
                + ", dishBaseHeight=" + value.dishBaseHeight()
                + ", soupBaseHeight=" + value.soupBaseHeight()
                + ", woodenMaxModelHeight=" + value.woodenMaxModelHeight()
                + ", porcelainMaxModelHeight=" + value.porcelainMaxModelHeight()
                + ", allowHandheldDishEating=" + value.allowHandheldDishEating()
                + ", allowHandheldSoupEating=" + value.allowHandheldSoupEating()
                + ", ingredientModelCollision=" + value.ingredientModelCollision()
                + ", modelMicroOffset=" + value.modelMicroOffset()
                + ", placementAnimation=" + value.placementAnimation()
                + ", placementPreviewAlpha=" + value.placementPreviewAlpha()
                + ", wrappingBagIngredientPreview=" + value.wrappingBagIngredientPreview()
                + ", lunchBoxIngredientPreview=" + value.lunchBoxIngredientPreview()
                + ", debug=" + value.debugLogging();
    }

    private ConfigManager() {
    }
}
