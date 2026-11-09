package com.alipro49.free_server;

import com.alipro49.free_server.config.FreeServerConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Free_server implements ModInitializer {
	public static final String MOD_ID = "free_server";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[FreeServer] Mod yuklendi.");
		FreeServerConfig.load();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}