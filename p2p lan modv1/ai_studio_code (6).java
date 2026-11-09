package com.alipro49.free_server.mixin;

import com.alipro49.free_server.config.FreeServerConfig;
import com.alipro49.free_server.network.HostManager;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
	@Inject(method = "usesAuthentication", at = @At("HEAD"), cancellable = true)
	private void freeServer$overrideOnlineMode(CallbackInfoReturnable<Boolean> cir) {
		if (HostManager.getInstance().isHostingActive()) {
			cir.setReturnValue(FreeServerConfig.isOnlineMode());
		}
	}
}