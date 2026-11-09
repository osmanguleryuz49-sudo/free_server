package com.alipro49.free_server.client;

import com.alipro49.free_server.network.HostManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public class Free_serverClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("freeserver")
					.then(ClientCommandManager.literal("approve")
							.then(ClientCommandManager.argument("sessionId", IntegerArgumentType.integer(1))
									.executes(context -> {
										int sid = IntegerArgumentType.getInteger(context, "sessionId");
										HostManager.getInstance().approvePlayer(sid);
										context.getSource().sendFeedback(Component.literal("§a[FreeServer] Oyuncu onaylandi (ID: " + sid + ")."));
										return 1;
									})))
					.then(ClientCommandManager.literal("reject")
							.then(ClientCommandManager.argument("sessionId", IntegerArgumentType.integer(1))
									.executes(context -> {
										int sid = IntegerArgumentType.getInteger(context, "sessionId");
										HostManager.getInstance().rejectPlayer(sid, "Sunucu sahibi isteginizi reddetti.");
										context.getSource().sendFeedback(Component.literal("§c[FreeServer] Katilma istegi reddedildi (ID: " + sid + ")."));
										return 1;
									}))));
		});
	}
}