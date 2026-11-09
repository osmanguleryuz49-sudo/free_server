package com.alipro49.free_server.client.mixin;

import com.alipro49.free_server.client.gui.JoinWithCodeScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
	protected JoinMultiplayerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void freeServer$addJoinWithCodeButton(CallbackInfo ci) {
		this.addRenderableWidget(Button.builder(Component.translatable("free_server.gui.join_with_code"), btn -> {
			this.minecraft.setScreen(new JoinWithCodeScreen(this));
		}).bounds(this.width / 2 - 154, 10, 100, 20).build());
	}
}