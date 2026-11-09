package com.alipro49.free_server.client.mixin;

import com.alipro49.free_server.client.gui.HostControlScreen;
import com.alipro49.free_server.network.HostManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void freeServer$addHostPanelButton(CallbackInfo ci) {
		if (HostManager.getInstance().isHostingActive()) {
			int centerX = this.width / 2;
			this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.host_panel"), btn -> {
				this.minecraft.setScreen(new HostControlScreen(this));
			}).bounds(centerX - 102, 38, 204, 20).build());
		}
	}
}