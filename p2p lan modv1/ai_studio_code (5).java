package com.alipro49.free_server.network;

import com.alipro49.free_server.Free_server;
import com.alipro49.free_server.config.FreeServerConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class ClientProxyManager {
	private static final ClientProxyManager INSTANCE = new ClientProxyManager();
	public static ClientProxyManager getInstance() {
		return INSTANCE;
	}

	private ServerSocket proxyServer;
	private Socket clientSocket;
	private WebSocket webSocket;
	private volatile boolean active = false;

	private ClientProxyManager() {}

	public CompletableFuture<Integer> connectWithCode(String code, Consumer<String> statusListener) {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		stop();

		try {
			proxyServer = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
			int localPort = proxyServer.getLocalPort();
			active = true;

			statusListener.accept("free_server.gui.status.connecting");

			HttpClient client = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(12))
					.build();

			String url = FreeServerConfig.getRelayUrl();
			if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
				url = "wss://" + url;
			}

			client.newWebSocketBuilder()
					.buildAsync(URI.create(url), new WebSocket.Listener() {
						@Override
						public void onOpen(WebSocket ws) {
							ClientProxyManager.this.webSocket = ws;

							Minecraft mc = Minecraft.getInstance();
							JsonObject joinMsg = new JsonObject();
							joinMsg.addProperty("action", RelayProtocol.ACTION_PLAYER_JOIN);
							joinMsg.addProperty("code", code.trim().toUpperCase());
							joinMsg.addProperty("playerName", mc.getUser().getName());
							joinMsg.addProperty("playerUuid", mc.getUser().getProfileId().toString());

							ws.sendText(joinMsg.toString(), true);
							ws.request(1);
						}

						@Override
						public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
							try {
								JsonObject json = JsonParser.parseString(data.toString()).getAsJsonObject();
								String action = json.get("action").getAsString();

								switch (action) {
									case RelayProtocol.ACTION_WAITING_APPROVAL -> {
										statusListener.accept("free_server.gui.status.waiting_approval");
									}
									case RelayProtocol.ACTION_JOIN_APPROVED -> {
										statusListener.accept("free_server.gui.status.approved");
										startLocalSocketListener();
										future.complete(localPort);
									}
									case RelayProtocol.ACTION_JOIN_REJECTED -> {
										String reason = json.has("reason") ? json.get("reason").getAsString() : "Reddedildi";
										future.completeExceptionally(new RuntimeException(reason));
										stop();
									}
									case RelayProtocol.ACTION_ERROR -> {
										String msg = json.get("message").getAsString();
										future.completeExceptionally(new RuntimeException(msg));
										stop();
									}
									case RelayProtocol.ACTION_HOST_CLOSED, RelayProtocol.ACTION_KICKED -> {
										stop();
									}
								}
							} catch (Exception e) {
								Free_server.LOGGER.error("Istemci kontrol mesaji hatasi", e);
							}
							ws.request(1);
							return null;
						}

						@Override
						public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
							if (clientSocket != null && !clientSocket.isClosed()) {
								try {
									OutputStream out = clientSocket.getOutputStream();
									byte[] buf = new byte[data.remaining()];
									data.get(buf);
									out.write(buf);
									out.flush();
								} catch (Exception e) {
									stop();
								}
							}
							ws.request(1);
							return null;
						}

						@Override
						public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
							stop();
							return null;
						}

						@Override
						public void onError(WebSocket ws, Throwable error) {
							if (!future.isDone()) {
								future.completeExceptionally(error);
							}
							stop();
						}
					});

		} catch (Exception e) {
			future.completeExceptionally(e);
			stop();
		}

		return future;
	}

	private void startLocalSocketListener() {
		Thread.startVirtualThread(() -> {
			try {
				clientSocket = proxyServer.accept();
				proxyServer.close();

				InputStream in = clientSocket.getInputStream();
				byte[] buffer = new byte[16384];
				int n;

				while (active && (n = in.read(buffer)) != -1) {
					if (webSocket != null) {
						webSocket.sendBinary(ByteBuffer.wrap(buffer, 0, n), true);
					}
				}
			} catch (Exception ignored) {
			} finally {
				stop();
			}
		});
	}

	public void stop() {
		active = false;
		try {
			if (proxyServer != null && !proxyServer.isClosed()) {
				proxyServer.close();
			}
		} catch (Exception ignored) {}
		proxyServer = null;

		try {
			if (clientSocket != null && !clientSocket.isClosed()) {
				clientSocket.close();
			}
		} catch (Exception ignored) {}
		clientSocket = null;

		if (webSocket != null) {
			try {
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Istemci kapandi").join();
			} catch (Exception ignored) {}
			webSocket = null;
		}
	}
}