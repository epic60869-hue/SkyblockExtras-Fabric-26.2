package com.skyblockextras.rng;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockextras.config.SbeConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Sends one editable Discord webhook message for the current Minecraft session. */
public final class DiscordWebhook {
    private final SbeConfig config;
    private final long sessionStart;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private volatile String messageId = "";
    private volatile boolean creatingMessage;

    public DiscordWebhook(SbeConfig config, long sessionStart) {
        this.config = config;
        this.sessionStart = sessionStart;
    }

    public void recordDrop(String item) {
        if (!enabled() || item == null || item.isBlank()) return;
        synchronized (counts) {
            counts.merge(item, 1, Integer::sum);
        }
        updateMessage();
    }

    public void test() {
        if (!enabled()) return;
        post(createWebhookUrl(true), payload("SBE Webhook Test", "Discord webhook is working."));
    }

    public void resetForNewWebhook() {
        messageId = "";
        creatingMessage = false;
        synchronized (counts) { counts.clear(); }
    }

    private boolean enabled() {
        return config.discordWebhookEnabled && validUrl(config.discordWebhookUrl);
    }

    private void updateMessage() {
        String body = payload("🌾 SBE RNG Session", buildDescription());
        String id = messageId;
        if (id == null || id.isBlank()) {
            synchronized (this) {
                if (creatingMessage || !messageId.isBlank()) return;
                creatingMessage = true;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(createWebhookUrl(true)))
                            .timeout(Duration.ofSeconds(8))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
                    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() / 100 == 2) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            if (json.has("id")) messageId = json.get("id").getAsString();
                        } catch (Exception ignored) { }
                    }
                } catch (Exception ignored) {
                } finally {
                    creatingMessage = false;
                }
            });
            return;
        }
        patch(id, body);
    }

    private String buildDescription() {
        StringBuilder out = new StringBuilder();
        out.append("**Session Uptime:** `").append(formatUptime(System.currentTimeMillis() - sessionStart)).append("`\n\n");
        out.append("**Drops**\n");
        synchronized (counts) {
            if (counts.isEmpty()) out.append("No tracked drops yet.");
            else counts.forEach((item, count) -> out.append("• ").append(item).append(" ×").append(count).append("\n"));
        }
        return out.toString();
    }

    private String payload(String title, String description) {
        String safeTitle = jsonEscape(title);
        String safeDescription = jsonEscape(description);
        return "{\"embeds\":[{\"title\":\"" + safeTitle + "\",\"description\":\"" + safeDescription + "\",\"color\":16753920}]}";
    }

    private void post(String url, String body) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) { }
        });
    }

    private void patch(String id, String body) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(createWebhookUrl(false) + "/messages/" + id))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) { }
        });
    }

    private String createWebhookUrl(boolean wait) {
        String url = config.discordWebhookUrl.trim();
        if (!wait) return url;
        return url + (url.contains("?") ? "&wait=true" : "?wait=true");
    }

    private static boolean validUrl(String url) {
        return url != null && url.startsWith("https://discord.com/api/webhooks/") && url.length() > 45;
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }

    private static String formatUptime(long millis) {
        long total = Math.max(0L, millis / 1000L);
        long days = total / 86400L;
        total %= 86400L;
        long hours = total / 3600L;
        total %= 3600L;
        long minutes = total / 60L;
        long seconds = total % 60L;
        if (days > 0) return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
}
