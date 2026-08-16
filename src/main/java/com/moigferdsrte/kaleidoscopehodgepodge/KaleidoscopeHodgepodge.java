package com.moigferdsrte.kaleidoscopehodgepodge;

import com.moigferdsrte.kaleidoscopehodgepodge.config.ConfigManager;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHCreativeModeTabs;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHMenus;
import com.moigferdsrte.kaleidoscopehodgepodge.interaction.PackingBagRotationHandler;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KaleidoscopeHodgepodge.MOD_ID)
public final class KaleidoscopeHodgepodge {
	public static final String MOD_ID = "kaleidoscope_hodgepodge";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public KaleidoscopeHodgepodge(IEventBus modBus, ModContainer modContainer) {
		CrashDiagnostics.install();
		KHDataComponents.init(modBus);
		KHBlocks.init(modBus);
		KHBlockEntities.init(modBus);
		KHItems.init(modBus);
		KHMenus.init(modBus);
		KHCreativeModeTabs.init(modBus);
		PackingBagRotationHandler.init();
		ConfigManager.start(modBus, modContainer);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			KaleidoscopeHodgepodgeClient.register(modBus);
		}
		LOGGER.info("Loading Kaleidoscope-Hodgepodge");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
