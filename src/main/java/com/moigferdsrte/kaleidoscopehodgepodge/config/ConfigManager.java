package com.moigferdsrte.kaleidoscopehodgepodge.config;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class ConfigManager {
    private static final String COMMON_FILE = KaleidoscopeHodgepodge.MOD_ID + "-common.toml";
    private static final String CLIENT_FILE = KaleidoscopeHodgepodge.MOD_ID + "-client.toml";
    private static boolean started;

    public static synchronized void start(IEventBus modBus, ModContainer modContainer) {
        if (started) return;
        started = true;
        modContainer.registerConfig(ModConfig.Type.COMMON, GeneralConfig.COMMON_SPEC, COMMON_FILE);
        modContainer.registerConfig(ModConfig.Type.CLIENT, GeneralConfig.CLIENT_SPEC, CLIENT_FILE);
        modBus.addListener((ModConfigEvent.Loading event) -> reload(event.getConfig()));
        modBus.addListener((ModConfigEvent.Reloading event) -> reload(event.getConfig()));
        KaleidoscopeHodgepodge.LOGGER.info("Registered NeoForge configuration");
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
                + ", allowHandheldFeastEating=" + value.allowHandheldFeastEating()
                + ", modelMicroOffset=" + value.modelMicroOffset()
                + ", placementAnimation=" + value.placementAnimation()
                + ", debug=" + value.debugLogging();
    }

    private ConfigManager() {
    }
}
