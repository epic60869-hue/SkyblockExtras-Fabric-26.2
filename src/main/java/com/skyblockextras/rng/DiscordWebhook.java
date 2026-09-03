package com.skyblockextras.rng;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockextras.config.SbeConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sends one editable Discord webhook message for the current Minecraft session. */
public final class DiscordWebhook {
    private static final Pattern TOOL_CAPSULE = Pattern.compile(
            "(?i)^\\s*OVERFLOW!\\s*Your\\s+(.+?)\\s+has\\s+just\\s+dropped\\s+a\\s+Tool\\s+Exp\\s+Capsule!\\s*\\(\\s*Level\\s*(\\d+)\\s*\\)\\s*$"
    );

    private final SbeConfig config;
    private final long sessionStart;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final PriceService prices = new PriceService();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, Integer> toolLevels = new LinkedHashMap<>();
    private volatile String messageId = "";
    private volatile boolean creatingMessage;
    private volatile String lastDrop = "";
    private volatile String lastTool = "";
    private volatile int toolLevelsGained = 0;

    public DiscordWebhook(SbeConfig config, long sessionStart) {
        this.config = config;
        this.sessionStart = sessionStart;
    }

    public void recordDrop(String item) {
        if (!enabled() || item == null || item.isBlank()) return;
        synchronized (counts) {
            counts.merge(item, 1, Integer::sum);
        }
        lastDrop = item;
        updateMessage();
    }

    /** Parses the Hypixel Tool Exp Capsule overflow message. */
    public void handleChatMessage(String message) {
        if (!enabled() || message == null || message.isBlank()) return;
        String clean = RngMessageMatcher.stripMinecraftFormatting(message);
        Matcher matcher = TOOL_CAPSULE.matcher(clean);
        if (!matcher.matches()) return;

        String tool = matcher.group(1).trim();
        int level;
        try {
            level = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException ignored) {
            return;
        }

        synchronized (toolLevels) {
            Integer previous = toolLevels.get(tool);
            if (previous != null && level > previous) {
                toolLevelsGained += level - previous;
            }
            toolLevels.put(tool, level);
        }
        lastTool = tool;
        counts.merge("Tool Exp Capsule", 1, Integer::sum);
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
        synchronized (toolLevels) { toolLevels.clear(); }
        toolLevelsGained = 0;
        lastDrop = "";
        lastTool = "";
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
        out.append("**Session Uptime:** `").append(formatUptime(System.currentTimeMillis() - sessionStart)).append("`\\n\\n");

        out.append("**Drops**\\n");
        double totalValue = 0.0D;
        synchronized (counts) {
            if (counts.isEmpty()) {
                out.append("No tracked drops yet.\\n");
            } else {
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String item = entry.getKey();
                    int count = entry.getValue();
                    Double unit = prices.cachedOrUnknown(item);
                    if (unit != null && unit > 0) {
                        double subtotal = unit * count;
                        totalValue += subtotal;
                        out.append("• ").append(item).append(" ×").append(count)
                                .append(" — ").append(formatPrice(unit)).append(" each")
                                .append(" — **").append(formatPrice(subtotal)).append("**\\n");
                    } else {
                        out.append("• ").append(item).append(" ×").append(count).append(" — `--`").append("\\n");
                    }
                }
            }
        }

        out.append("\\n**Total Value:** **").append(formatPrice(totalValue)).append("**\\n");

        synchronized (toolLevels) {
            if (!toolLevels.isEmpty()) {
                out.append("\\n**Tool Levels Gained:** +").append(toolLevelsGained).append("\\n");
                if (!lastTool.isBlank()) {
                    Integer level = toolLevels.get(lastTool);
                    if (level != null) out.append("**Tool Level:** ").append(level).append(" (`").append(lastTool).append("`)\\n");
                }
            }
        }

        if (!lastDrop.isBlank()) out.append("\\n**Last Drop:** ").append(lastDrop);
        return out.toString();
    }

    private String payload(String title, String description) {
        return "{\"embeds\":[{\"title\":\"" + jsonEscape(title) + "\",\"description\":\"" + jsonEscape(description) + "\",\"color\":16753920}]}";
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

    private static String formatPrice(double value) {
        if (value <= 0) return "0 coins";
        if (value >= 1_000_000_000D) return String.format(Locale.US, "%.2fB coins", value / 1_000_000_000D);
        if (value >= 1_000_000D) return String.format(Locale.US, "%.2fM coins", value / 1_000_000D);
        if (value >= 1_000D) return String.format(Locale.US, "%.2fK coins", value / 1_000D);
        return String.format(Locale.US, "%,.0f coins", value);
    }

    private final class PriceService {
        private final Map<String, Double> cache = new ConcurrentHashMap<>();
        private final Map<String, Long> cacheTimes = new ConcurrentHashMap<>();
        private volatile String resourceItems = "";
        private volatile long resourceFetchedAt = 0L;

        Double cachedOrUnknown(String displayName) {
            String key = normalize(displayName);
            Double value = cache.get(key);
            if (value != null && System.currentTimeMillis() - cacheTimes.getOrDefault(key, 0L) < 60_000L) return value;
            lookup(displayName);
            return value;
        }

        CompletableFuture<Double> lookup(String displayName) {
            String key = normalize(displayName);
            Double cached = cache.get(key);
            if (cached != null && System.currentTimeMillis() - cacheTimes.getOrDefault(key, 0L) < 60_000L) return CompletableFuture.completedFuture(cached);
            return ensureResources().thenCompose(v -> {
                String id = findItemId(displayName, resourceItems);
                if (id == null) id = fallbackId(displayName);
                final String finalId = id;
                if (finalId == null || finalId.isBlank()) return CompletableFuture.completedFuture(null);
                return get("https://api.hypixel.net/v2/skyblock/bazaar")
                        .thenApply(body -> parseBazaar(body, finalId))
                        .thenCompose(bazaar -> {
                            if (bazaar != null && bazaar > 0) return CompletableFuture.completedFuture(bazaar);
                            return get("https://lb.tricked.dev/lowestbin/" + finalId).thenApply(this::parseSinglePrice);
                        });
            }).thenApply(value -> {
                if (value != null && value > 0) { cache.put(key, value); cacheTimes.put(key, System.currentTimeMillis()); updateMessage(); }
                return value;
            });
        }

        private CompletableFuture<Void> ensureResources() {
            long now = System.currentTimeMillis();
            if (!resourceItems.isBlank() && now - resourceFetchedAt < 6 * 60 * 60 * 1000L) return CompletableFuture.completedFuture(null);
            return get("https://api.hypixel.net/v2/resources/skyblock/items").thenAccept(body -> {
                if (body != null && !body.isBlank()) { resourceItems = body; resourceFetchedAt = System.currentTimeMillis(); }
            });
        }

        private CompletableFuture<String> get(String url) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "SkyblockExtras/0.1.2").GET().build();
            return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).exceptionally(e -> "");
        }

        private String findItemId(String name, String json) {
            if (json == null || json.isBlank()) return null;
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject items = root.has("items") && root.get("items").isJsonObject() ? root.getAsJsonObject("items") : root;
                String target = normalize(name);
                for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
                    if (!entry.getValue().isJsonObject()) continue;
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    if (obj.has("name") && target.equals(normalize(obj.get("name").getAsString()))) return entry.getKey();
                }
            } catch (Exception ignored) { }
            return null;
        }

        private String fallbackId(String name) { return name == null || name.isBlank() ? null : name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", ""); }

        private Double parseBazaar(String json, String id) {
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject products = root.getAsJsonObject("products");
                if (products == null || !products.has(id)) return null;
                JsonObject quick = products.getAsJsonObject(id).getAsJsonObject("quick_status");
                if (quick == null) return null;
                if (quick.has("buyPrice")) return quick.get("buyPrice").getAsDouble();
                if (quick.has("sellPrice")) return quick.get("sellPrice").getAsDouble();
            } catch (Exception ignored) { }
            return null;
        }

        private Double parseSinglePrice(String body) {
            try {
                String value = body == null ? "" : body.trim();
                if (value.isBlank()) return null;
                JsonElement parsed = JsonParser.parseString(value);
                if (parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isNumber()) return parsed.getAsDouble();
                if (parsed.isJsonObject()) {
                    JsonObject obj = parsed.getAsJsonObject();
                    for (String key : new String[]{"price", "lowest", "starting_bid", "bin"}) if (obj.has(key) && obj.get(key).isJsonPrimitive()) return obj.get(key).getAsDouble();
                }
                return Double.parseDouble(value.replaceAll("[^0-9.\\-]", ""));
            } catch (Exception ignored) { return null; }
        }

        private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    }
}
