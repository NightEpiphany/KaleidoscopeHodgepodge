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
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KaleidoscopeHodgepodge implements ModInitializer {
	public static final String MOD_ID = "kaleidoscope_hodgepodge";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CrashDiagnostics.install();
		KHDataComponents.init();
		KHBlocks.init();
		KHBlockEntities.init();
		KHItems.init();
		KHMenus.init();
		KHCreativeModeTabs.init();
		PackingBagRotationHandler.init();
		ConfigManager.start();
		LOGGER.info("Loading Kaleidoscope-Hodgepodge");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
