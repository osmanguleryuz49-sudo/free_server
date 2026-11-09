package com.alipro49.free_server.client.gui;

import com.alipro49.free_server.client.util.ConnectHelper;
import com.alipro49.free_server.network.ClientProxyManager;
import com.alipro49.free_server.network.WakeUpHelper;
import com.alipro49.free_server.config.FreeServerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class JoinWithCodeScreen extends Screen {
	private final Screen parent;
	private EditBox codeInput;
	private Button joinButton;
	private String statusKey = "";
	private String errorMessage = "";

	public JoinWithCodeScreen(Screen parent) {
		super(Component.translatable("free_server.gui.join_with_code"));
		this.parent = parent;
		WakeUpHelper.pingHealth(FreeServerConfig.getRelayUrl());
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;

		this.codeInput = new EditBox(this.font, centerX - 100, 75, 200, 20, Component.literal("Code"));
		this.codeInput.setMaxLength(10);
		this.codeInput.setResponder(text -> {
			String clean = text.trim().toUpperCase();
			if (!clean.equals(text)) {
				this.codeInput.setValue(clean);
			}
			this.joinButton.active = clean.length() >= 8;
		});
		this.addRenderableWidget(this.codeInput);

		this.joinButton = Button.builder(Component.translatable("free_server.gui.join"), btn -> {
			startJoin();
		}).bounds(centerX - 105, 115, 100, 20).build();
		this.joinButton.active = false;
		this.addRenderableWidget(this.joinButton);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> {
			ClientProxyManager.getInstance().stop();
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX + 5, 115, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.config"), btn -> {
			this.minecraft.setScreen(new ConfigScreen(this));
		}).bounds(centerX - 105, this.height - 35, 210, 20).build());
	}

	private void startJoin() {
		String code = this.codeInput.getValue().trim();
		if (code.isEmpty()) return;

		this.errorMessage = "";
		this.statusKey = "free_server.gui.status.connecting";
		this.joinButton.active = false;

		ClientProxyManager.getInstance().connectWithCode(code, newStatusKey -> {
			this.statusKey = newStatusKey;
		}).thenAccept(localPort -> {
			this.minecraft.execute(() -> {
				ConnectHelper.connect(this.parent, this.minecraft, "127.0.0.1", localPort);
			});
		}).exceptionally(ex -> {
			this.minecraft.execute(() -> {
				this.errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
				this.statusKey = "";
				this.joinButton.active = true;
			});
			return null;
		});
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		int centerX = this.width / 2;
		graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("free_server.gui.enter_code"), centerX, 55, 0xAAAAAA);

		if (!this.statusKey.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable(this.statusKey), centerX, 150, 0xFFFF55);
		}

		if (!this.errorMessage.isEmpty()) {
			graphics.drawCenteredString(this.font, "§c" + this.errorMessage, centerX, 150, 0xFF5555);
		}
	}
}