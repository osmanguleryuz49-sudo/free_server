package com.alipro49.free_server.client.gui;

import com.alipro49.free_server.config.FreeServerConfig;
import com.alipro49.free_server.network.HostManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

public class HostControlScreen extends Screen {
	private final Screen parent;
	private String copyNotification = "";
	private long copyNotificationTime = 0;

	public HostControlScreen(Screen parent) {
		super(Component.translatable("free_server.gui.host_panel"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		String code = HostManager.getInstance().getCurrentCode();
		if (code == null) code = "----";

		final String currentCodeToCopy = code;
		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.copy_code"), btn -> {
			this.minecraft.keyboardHandler.setClipboard(currentCodeToCopy);
			this.copyNotification = Component.translatable("free_server.gui.copied").getString();
			this.copyNotificationTime = System.currentTimeMillis();
		}).bounds(centerX - 105, 75, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.regen_code"), btn -> {
			HostManager.getInstance().regenerateCode();
		}).bounds(centerX + 5, 75, 100, 20).build());

		this.addRenderableWidget(Button.builder(getOnlineModeButtonText(), btn -> {
			boolean newMode = !FreeServerConfig.isOnlineMode();
			FreeServerConfig.setOnlineMode(newMode);
			btn.setMessage(getOnlineModeButtonText());

			if (this.minecraft.getSingleplayerServer() != null) {
				this.minecraft.getSingleplayerServer().setUsesAuthentication(newMode);
			}
		}).bounds(centerX - 105, 105, 210, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.stop_hosting"), btn -> {
			HostManager.getInstance().stopHosting();
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX - 105, this.height - 55, 210, 20).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, btn -> {
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX - 105, this.height - 30, 210, 20).build());

		int reqY = 175;
		List<HostManager.PendingRequest> requests = HostManager.getInstance().getPendingRequests();
		for (HostManager.PendingRequest req : requests) {
			int sid = req.sessionId;
			this.addRenderableWidget(Button.builder(Component.literal("✔ " + req.playerName), btn -> {
				HostManager.getInstance().approvePlayer(sid);
				this.rebuildWidgets();
			}).bounds(centerX - 105, reqY, 100, 18).build());

			this.addRenderableWidget(Button.builder(Component.literal("✖"), btn -> {
				HostManager.getInstance().rejectPlayer(sid, "Reddedildi");
				this.rebuildWidgets();
			}).bounds(centerX + 5, reqY, 40, 18).build());

			reqY += 22;
			if (reqY > this.height - 85) break;
		}

		int playerY = Math.max(reqY + 15, 200);
		List<HostManager.PlayerSession> sessions = HostManager.getInstance().getActiveSessions();
		for (HostManager.PlayerSession session : sessions) {
			int sid = session.sessionId;
			this.addRenderableWidget(Button.builder(Component.literal(session.playerName + " (At)"), btn -> {
				HostManager.getInstance().kickPlayer(sid, "Sunucu sahibi tarafindan atildiniz.");
				this.rebuildWidgets();
			}).bounds(centerX - 105, playerY, 210, 18).build());

			playerY += 22;
			if (playerY > this.height - 85) break;
		}
	}

	private Component getOnlineModeButtonText() {
		Component state = FreeServerConfig.isOnlineMode()
				? Component.translatable("free_server.gui.online_mode.on")
				: Component.translatable("free_server.gui.online_mode.off");
		return Component.translatable("free_server.gui.online_mode").append(state);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		int centerX = this.width / 2;
		graphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);

		String code = HostManager.getInstance().getCurrentCode();
		if (code == null) code = "Yukleniyor...";
		graphics.drawCenteredString(this.font, Component.translatable("free_server.gui.join_code"), centerX, 35, 0xAAAAAA);
		graphics.drawCenteredString(this.font, "§e§l" + code, centerX, 50, 0xFFFF55);

		if (System.currentTimeMillis() - copyNotificationTime < 2500 && !copyNotification.isEmpty()) {
			graphics.drawCenteredString(this.font, "§a" + copyNotification, centerX, 97, 0x55FF55);
		}

		if (!FreeServerConfig.isOnlineMode()) {
			graphics.drawCenteredString(this.font, "§c§o" + Component.translatable("free_server.gui.offline_warning").getString(), centerX, 130, 0xFF5555);
		}

		graphics.drawCenteredString(this.font, Component.translatable("free_server.gui.players"), centerX, 155, 0xAAAAAA);
		if (HostManager.getInstance().getActiveSessions().isEmpty() && HostManager.getInstance().getPendingRequests().isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("free_server.gui.no_players"), centerX, 175, 0x777777);
		}
	}
}