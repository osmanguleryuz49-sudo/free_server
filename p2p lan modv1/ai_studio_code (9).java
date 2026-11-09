package com.alipro49.free_server.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ConnectHelper {
	public static void connect(Screen parent, Minecraft minecraft, String host, int port) {
		ServerAddress address = new ServerAddress(host, port);
		ServerData serverData = new ServerData("FreeServer Local", host + ":" + port, ServerData.Type.OTHER);

		for (Method method : ConnectScreen.class.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers()) && method.getName().equals("startConnecting")) {
				Class<?>[] params = method.getParameterTypes();
				try {
					if (params.length == 5 && Screen.class.isAssignableFrom(params[0])) {
						method.invoke(null, parent, minecraft, address, serverData, false);
						return;
					} else if (params.length == 6 && Screen.class.isAssignableFrom(params[0])) {
						method.invoke(null, parent, minecraft, address, serverData, false, null);
						return;
					}
				} catch (Exception ignored) {}
			}
		}

		minecraft.setScreen(parent);
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(Component.literal("§cBaglanti baslatilamadi."), false);
		}
	}
}