package com.alipro49.free_server.client.mixin;

import com.alipro49.free_server.client.gui.HostControlScreen;
import com.alipro49.free_server.network.HostManager;
import com.alipro49.free_server.network.WakeUpHelper;
import com.alipro49.free_server.config.FreeServerConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShareToLanScreen.class)
public abstract class ShareToLanScreenMixin extends Screen {
	@Shadow private GameType gameMode;
	@Shadow private boolean commands;

	protected ShareToLanScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void freeServer$addOpenToInternetButton(CallbackInfo ci) {
		WakeUpHelper.pingHealth(FreeServerConfig.getRelayUrl());

		int centerX = this.width / 2;
		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.open_to_internet"), btn -> {
			IntegratedServer server = this.minecraft.getSingleplayerServer();
			if (server != null) {
				int availablePort = HttpUtil.getAvailablePort();
				server.publishServer(this.gameMode, this.commands, availablePort);
				server.setUsesAuthentication(FreeServerConfig.isOnlineMode());

				HostManager.getInstance().startHosting(server.getPort()).thenAccept(code -> {
					this.minecraft.execute(() -> {
						this.minecraft.setScreen(new HostControlScreen(null));
					});
				}).exceptionally(ex -> {
					this.minecraft.execute(() -> {
						if (this.minecraft.player != null) {
							this.minecraft.player.displayClientMessage(
									Component.literal("§cRoleye baglanilamadi: " + ex.getMessage()), false
							);
						}
					});
					return null;
				});
			}
		}).bounds(centerX - 155, this.height - 54, 150, 20).build());
	}
}