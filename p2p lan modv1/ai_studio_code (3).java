package com.alipro49.free_server.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class WakeUpHelper {
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	public static CompletableFuture<Boolean> pingHealth(String websocketUrl) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String httpUrl = websocketUrl
						.replaceFirst("(?i)^wss://", "https://")
						.replaceFirst("(?i)^ws://", "http://");

				if (!httpUrl.endsWith("/")) {
					httpUrl += "/";
				}
				httpUrl += "health";

				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(httpUrl))
						.timeout(Duration.ofSeconds(15))
						.GET()
						.build();

				HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
				return response.statusCode() == 200;
			} catch (Exception e) {
				return false;
			}
		});
	}
}