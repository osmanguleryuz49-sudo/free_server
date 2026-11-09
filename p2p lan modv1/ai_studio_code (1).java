package com.alipro49.free_server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class FreeServerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("free_server.json").toFile();

	public static final String DEFAULT_RELAY_URL = "wss://free-server-relay.onrender.com";

	private static String relayUrl = DEFAULT_RELAY_URL;
	private static boolean onlineMode = true;
	private static int maxPlayers = 8;

	public static void load() {
		if (!CONFIG_FILE.exists()) {
			save();
			return;
		}

		try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			if (json.has("relayUrl")) {
				relayUrl = json.get("relayUrl").getAsString();
			}
			if (json.has("onlineMode")) {
				onlineMode = json.get("onlineMode").getAsBoolean();
			}
			if (json.has("maxPlayers")) {
				maxPlayers = json.get("maxPlayers").getAsInt();
			}
		} catch (Exception e) {
			save();
		}
	}

	public static void save() {
		JsonObject json = new JsonObject();
		json.addProperty("relayUrl", relayUrl);
		json.addProperty("onlineMode", onlineMode);
		json.addProperty("maxPlayers", maxPlayers);

		try {
			if (!CONFIG_FILE.getParentFile().exists()) {
				CONFIG_FILE.getParentFile().mkdirs();
			}
			try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
			}
		} catch (Exception ignored) {}
	}

	public static String getRelayUrl() {
		return relayUrl;
	}

	public static void setRelayUrl(String url) {
		relayUrl = url;
		save();
	}

	public static boolean isOnlineMode() {
		return onlineMode;
	}

	public static void setOnlineMode(boolean mode) {
		onlineMode = mode;
		save();
	}

	public static int getMaxPlayers() {
		return maxPlayers;
	}
}