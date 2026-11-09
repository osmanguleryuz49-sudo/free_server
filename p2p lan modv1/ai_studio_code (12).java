package com.alipro49.free_server.client.gui;

import com.alipro49.free_server.config.FreeServerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
	private final Screen parent;
	private EditBox relayBox;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("free_server.gui.config"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;

		this.relayBox = new EditBox(this.font, centerX - 150, 80, 300, 20, Component.literal("Relay URL"));
		this.relayBox.setMaxLength(128);
		this.relayBox.setValue(FreeServerConfig.getRelayUrl());
		this.addRenderableWidget(this.relayBox);

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.save"), btn -> {
			String url = this.relayBox.getValue().trim();
			if (!url.isEmpty()) {
				FreeServerConfig.setRelayUrl(url);
			}
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX - 155, 120, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.reset"), btn -> {
			this.relayBox.setValue(FreeServerConfig.DEFAULT_RELAY_URL);
		}).bounds(centerX - 50, 120, 100, 20).build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> {
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX + 55, 120, 100, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		int centerX = this.width / 2;
		graphics.drawCenteredString(this.font, this.title, centerX, 25, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("free_server.gui.relay_url"), centerX, 60, 0xAAAAAA);
	}
}