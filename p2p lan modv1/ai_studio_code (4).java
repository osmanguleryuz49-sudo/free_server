package com.alipro49.free_server.network;

import com.alipro49.free_server.Free_server;
import com.alipro49.free_server.config.FreeServerConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class HostManager {
	private static final HostManager INSTANCE = new HostManager();
	public static HostManager getInstance() {
		return INSTANCE;
	}

	public static class PlayerSession {
		public final int sessionId;
		public final String playerName;
		public Socket socket;
		public OutputStream out;
		public volatile boolean closed = false;

		public PlayerSession(int sessionId, String playerName) {
			this.sessionId = sessionId;
			this.playerName = playerName;
		}
	}

	public static class PendingRequest {
		public final int sessionId;
		public final String playerName;

		public PendingRequest(int sessionId, String playerName) {
			this.sessionId = sessionId;
			this.playerName = playerName;
		}
	}

	private WebSocket webSocket;
	private int localLanPort = -1;
	private String currentCode = null;
	private boolean hostingActive = false;

	private final Map<Integer, PlayerSession> activeSessions = new ConcurrentHashMap<>();
	private final Map<Integer, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
	private final ExecutorService tunnelPool = Executors.newCachedThreadPool();

	private HostManager() {}

	public boolean isHostingActive() {
		return hostingActive && webSocket != null;
	}

	public String getCurrentCode() {
		return currentCode;
	}

	public List<PlayerSession> getActiveSessions() {
		return new ArrayList<>(activeSessions.values());
	}

	public List<PendingRequest> getPendingRequests() {
		return new ArrayList<>(pendingRequests.values());
	}

	public CompletableFuture<String> startHosting(int lanPort) {
		this.localLanPort = lanPort;
		CompletableFuture<String> future = new CompletableFuture<>();

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
						HostManager.this.webSocket = ws;
						HostManager.this.hostingActive = true;

						JsonObject createMsg = new JsonObject();
						createMsg.addProperty("action", RelayProtocol.ACTION_HOST_CREATE);
						createMsg.addProperty("onlineMode", FreeServerConfig.isOnlineMode());
						createMsg.addProperty("maxPlayers", FreeServerConfig.getMaxPlayers());
						ws.sendText(createMsg.toString(), true);
						ws.request(1);
					}

					@Override
					public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
						try {
							JsonObject json = JsonParser.parseString(data.toString()).getAsJsonObject();
							handleControlMessage(json, future);
						} catch (Exception e) {
							Free_server.LOGGER.error("Host mesaji isleme hatasi", e);
						}
						ws.request(1);
						return null;
					}

					@Override
					public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
						handleIncomingBinary(data);
						ws.request(1);
						return null;
					}

					@Override
					public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
						stopHosting();
						return null;
					}

					@Override
					public void onError(WebSocket ws, Throwable error) {
						if (!future.isDone()) {
							future.completeExceptionally(error);
						}
						stopHosting();
					}
				});

		return future;
	}

	private void handleControlMessage(JsonObject json, CompletableFuture<String> initialFuture) {
		String action = json.get("action").getAsString();

		switch (action) {
			case RelayProtocol.ACTION_HOST_CREATED -> {
				this.currentCode = json.get("code").getAsString();
				if (!initialFuture.isDone()) {
					initialFuture.complete(this.currentCode);
				}
			}
			case RelayProtocol.ACTION_CODE_REGENERATED -> {
				this.currentCode = json.get("code").getAsString();
			}
			case RelayProtocol.ACTION_PLAYER_REQUEST -> {
				int sessionId = json.get("sessionId").getAsInt();
				String playerName = json.get("playerName").getAsString();

				pendingRequests.put(sessionId, new PendingRequest(sessionId, playerName));
				notifyHostInChat(sessionId, playerName);
			}
			case RelayProtocol.ACTION_PLAYER_LEFT -> {
				int sessionId = json.get("sessionId").getAsInt();
				String playerName = json.has("playerName") ? json.get("playerName").getAsString() : "Oyuncu";
				closeSession(sessionId);
				pendingRequests.remove(sessionId);

				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.sendSystemMessage(Component.translatable("free_server.chat.player_left", playerName));
				}
			}
			case RelayProtocol.ACTION_ERROR -> {
				String msg = json.get("message").getAsString();
				if (!initialFuture.isDone()) {
					initialFuture.completeExceptionally(new RuntimeException(msg));
				}
			}
		}
	}

	private void notifyHostInChat(int sessionId, String playerName) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		mc.player.sendSystemMessage(Component.translatable("free_server.chat.join_request", playerName));

		Component acceptBtn = Component.translatable("free_server.chat.accept")
				.withStyle(Style.EMPTY
						.withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/freeserver approve " + sessionId)));

		Component rejectBtn = Component.translatable("free_server.chat.reject")
				.withStyle(Style.EMPTY
						.withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/freeserver reject " + sessionId)));

		mc.player.sendSystemMessage(Component.literal("  ").append(acceptBtn).append("   ").append(rejectBtn));
	}

	public void approvePlayer(int sessionId) {
		PendingRequest req = pendingRequests.remove(sessionId);
		String playerName = req != null ? req.playerName : "Oyuncu";

		tunnelPool.submit(() -> {
			try {
				Socket lanSocket = new Socket("127.0.0.1", localLanPort);
				PlayerSession session = new PlayerSession(sessionId, playerName);
				session.socket = lanSocket;
				session.out = lanSocket.getOutputStream();
				activeSessions.put(sessionId, session);

				JsonObject obj = new JsonObject();
				obj.addProperty("action", RelayProtocol.ACTION_HOST_DECISION);
				obj.addProperty("sessionId", sessionId);
				obj.addProperty("approved", true);
				sendText(obj.toString());

				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.sendSystemMessage(Component.translatable("free_server.chat.player_joined", playerName));
				}

				InputStream in = lanSocket.getInputStream();
				byte[] buffer = new byte[16384];
				int bytesRead;

				while (!session.closed && (bytesRead = in.read(buffer)) != -1) {
					byte[] wrapped = new byte[4 + bytesRead];
					wrapped[0] = (byte) (sessionId >> 24);
					wrapped[1] = (byte) (sessionId >> 16);
					wrapped[2] = (byte) (sessionId >> 8);
					wrapped[3] = (byte) (sessionId);
					System.arraycopy(buffer, 0, wrapped, 4, bytesRead);

					if (webSocket != null) {
						webSocket.sendBinary(ByteBuffer.wrap(wrapped), true);
					}
				}
			} catch (Exception ignored) {
			} finally {
				closeSession(sessionId);
			}
		});
	}

	public void rejectPlayer(int sessionId, String reason) {
		pendingRequests.remove(sessionId);
		JsonObject obj = new JsonObject();
		obj.addProperty("action", RelayProtocol.ACTION_HOST_DECISION);
		obj.addProperty("sessionId", sessionId);
		obj.addProperty("approved", false);
		obj.addProperty("reason", reason);
		sendText(obj.toString());
	}

	public void kickPlayer(int sessionId, String reason) {
		closeSession(sessionId);
		JsonObject obj = new JsonObject();
		obj.addProperty("action", RelayProtocol.ACTION_HOST_KICK);
		obj.addProperty("sessionId", sessionId);
		obj.addProperty("reason", reason);
		sendText(obj.toString());
	}

	public void regenerateCode() {
		JsonObject obj = new JsonObject();
		obj.addProperty("action", RelayProtocol.ACTION_HOST_REGEN_CODE);
		sendText(obj.toString());
	}

	private void handleIncomingBinary(ByteBuffer data) {
		if (data.remaining() < 4) return;
		int sessionId = data.getInt();
		PlayerSession session = activeSessions.get(sessionId);

		if (session != null && !session.closed && session.out != null) {
			try {
				byte[] payload = new byte[data.remaining()];
				data.get(payload);
				session.out.write(payload);
				session.out.flush();
			} catch (Exception e) {
				closeSession(sessionId);
			}
		}
	}

	public void closeSession(int sessionId) {
		PlayerSession session = activeSessions.remove(sessionId);
		if (session != null) {
			session.closed = true;
			try {
				if (session.socket != null && !session.socket.isClosed()) {
					session.socket.close();
				}
			} catch (Exception ignored) {}
		}
	}

	public void stopHosting() {
		hostingActive = false;
		currentCode = null;
		localLanPort = -1;

		for (Integer sid : activeSessions.keySet()) {
			closeSession(sid);
		}
		pendingRequests.clear();

		if (webSocket != null) {
			try {
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Host kapandi").join();
			} catch (Exception ignored) {}
			webSocket = null;
		}
	}

	private void sendText(String text) {
		if (webSocket != null) {
			webSocket.sendText(text, true);
		}
	}
}