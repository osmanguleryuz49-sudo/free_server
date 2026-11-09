package com.alipro49.free_server.client.gui;

import com.alipro49.free_server.network.HostManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ApprovalConfirmScreen extends Screen {
	private final Screen parent;
	private final int sessionId;
	private final String playerName;

	public ApprovalConfirmScreen(Screen parent, int sessionId, String playerName) {
		super(Component.translatable("free_server.gui.approval.title"));
		this.parent = parent;
		this.sessionId = sessionId;
		this.playerName = playerName;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.approval.accept"), btn -> {
			HostManager.getInstance().approvePlayer(this.sessionId);
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX - 105, centerY + 20, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.approval.reject"), btn -> {
			HostManager.getInstance().rejectPlayer(this.sessionId, "Sunucu sahibi tarafindan reddedildi.");
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX + 5, centerY + 20, 100, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		graphics.drawCenteredString(this.font, this.title, centerX, centerY - 40, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("free_server.gui.approval.prompt", this.playerName), centerX, centerY - 15, 0xFFFF55);
	}
}