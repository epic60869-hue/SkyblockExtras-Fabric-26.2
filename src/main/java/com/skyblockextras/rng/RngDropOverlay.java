package com.skyblockextras.rng;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Center-screen RNG announcement overlay with live Bazaar/LBIN prices and stack counts. */
public class RngDropOverlay {
    private final SbeConfig config;
    private final PriceService prices = new PriceService();

    private String item = "";
    private String price = "--";
    private double unitPrice = 0.0D;
    private int amount = 1;
    private long expiresAt = 0L;
    private long sequence = 0L;
    private long lastSameItemAt = 0L;

    public RngDropOverlay(SbeConfig config) { this.config = config; }

    public synchronized void show(String itemName) {
        if (!config.rngDropOverlayEnabled || itemName == null || itemName.isBlank()) return;
        long now = System.currentTimeMillis();
        boolean sameBurst = itemName.equals(item) && now - lastSameItemAt <= 3000L;
        if (sameBurst) amount++;
        else amount = 1;

        item = itemName;
        lastSameItemAt = now;
        expiresAt = now + 6000L;
        price = "--";
        long requestId = ++sequence;

        prices.lookup(itemName).thenAccept(value -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (requestId != sequence || value == null || value <= 0) return;
                unitPrice = value;
                price = formatPrice(value * amount);
            });
        });
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.rngDropOverlayEnabled || item.isBlank() || System.currentTimeMillis() >= expiresAt) return;

        Minecraft client = Minecraft.getInstance();
        float scale = Math.max(0.5f, Math.min(3.0f, config.rngDropOverlayScale));
        String countText = amount > 1 ? "x" + amount : "";
        String title = amount > 1 ? "RNG DROP!  " + countText : "RNG DROP!";
        String priceText = price.equals("--") ? "Value: --" : "Value: " + price;
        int itemWidth = client.font.width(item);
        int titleWidth = client.font.width(title);
        int priceWidth = client.font.width(priceText);
        int width = Math.max(240, Math.max(itemWidth, Math.max(titleWidth, priceWidth)) + 70);
        int height = 82;
        int scaledWidth = Math.round(width * scale);
        int scaledHeight = Math.round(height * scale);
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int x = config.rngDropOverlayX < 0 ? (screenWidth - scaledWidth) / 2 : config.rngDropOverlayX;
        int y = config.rngDropOverlayY < 0 ? (screenHeight - scaledHeight) / 2 : config.rngDropOverlayY;

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);

        if (config.rngDropOverlayBackgroundEnabled) {
            graphics.fill(-4, -4, width + 4, height + 4, 0x55200055);
            graphics.outline(-3, -3, width + 3, height + 3, 0xFFFF55FF);
            graphics.fill(0, 0, width, height, 0xEE101018);
            graphics.outline(0, 0, width, height, 0xFFFFA3FF);
            graphics.fill(1, 1, width - 1, 5, 0xFFFF55FF);
            graphics.fill(1, height - 5, width - 1, height - 1, 0xFF8A2BE2);
        }

        graphics.text(client.font, Component.literal(title), (width - titleWidth) / 2, 10, 0xFFFF55FF, true);
        graphics.text(client.font, Component.literal(item), (width - itemWidth) / 2, 31, 0xFFFFFFFF, true);
        graphics.text(client.font, Component.literal(priceText), (width - priceWidth) / 2, 52, 0xFFFFD45A, true);
        pose.popMatrix();
    }

    public boolean isVisible() { return config.rngDropOverlayEnabled && !item.isBlank() && System.currentTimeMillis() < expiresAt; }
    public String getItem() { return item; }
    public String getPrice() { return price; }
    public int getAmount() { return amount; }
    public double getUnitPrice() { return unitPrice; }

    private String formatPrice(double value) {
        return switch (config.rngDropPriceFormat == null ? "SHORT" : config.rngDropPriceFormat.toUpperCase(Locale.ROOT)) {
            case "FULL" -> String.format(Locale.ROOT, "%,.0f", value);
            case "COINS" -> String.format(Locale.ROOT, "%.2fM coins", value / 1_000_000D);
            default -> compact(value);
        };
    }

    private String compact(double value) {
        if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000D);
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", value / 1_000_000D);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.2fK", value / 1_000D);
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    private final class PriceService {
        private final HttpClient client = HttpClient.newBuilder().build();
        private final Map<String, Double> cache = new ConcurrentHashMap<>();
        private final Map<String, Long> cacheTimes = new ConcurrentHashMap<>();
        private volatile String resourceItems = "";
        private volatile long resourceFetchedAt = 0L;

        CompletableFuture<Double> lookup(String displayName) {
            String key = normalize(displayName);
            Double cached = cache.get(key);
            if (cached != null && System.currentTimeMillis() - cacheTimes.getOrDefault(key, 0L) < 60_000L) {
                return CompletableFuture.completedFuture(cached);
            }

            // First try Hypixel's official Bazaar data. If the item is not on
            // Bazaar, use Tricked's current lowest-BIN endpoint directly.
            return ensureResources().thenCompose(v -> {
                String id = findItemId(displayName, resourceItems);
                if (id == null) id = fallbackId(displayName);
                final String finalId = id;
                if (finalId == null || finalId.isBlank()) return CompletableFuture.completedFuture(null);

                return get("https://api.hypixel.net/v2/skyblock/bazaar")
                        .thenApply(body -> parseBazaar(body, finalId))
                        .thenCompose(bazaar -> {
                            if (bazaar != null && bazaar > 0) return CompletableFuture.completedFuture(bazaar);
                            return get("https://lb.tricked.dev/lowestbin/" + finalId)
                                    .thenApply(this::parseSinglePrice);
                        });
            }).thenApply(value -> {
                if (value != null && value > 0) {
                    cache.put(key, value);
                    cacheTimes.put(key, System.currentTimeMillis());
                }
                return value;
            });
        }

        private CompletableFuture<Void> ensureResources() {
            long now = System.currentTimeMillis();
            if (!resourceItems.isBlank() && now - resourceFetchedAt < 6 * 60 * 60 * 1000L) {
                return CompletableFuture.completedFuture(null);
            }
            return get("https://api.hypixel.net/v2/resources/skyblock/items").thenAccept(body -> {
                if (body != null && !body.isBlank()) {
                    resourceItems = body;
                    resourceFetchedAt = System.currentTimeMillis();
                }
            });
        }

        private CompletableFuture<String> get(String url) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "SkyblockExtras/0.1.2")
                    .GET().build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .exceptionally(e -> "");
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

        private String fallbackId(String name) {
            if (name == null || name.isBlank()) return null;
            return name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        }

        private Double parseBazaar(String json, String id) {
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject products = root.getAsJsonObject("products");
                if (products == null || !products.has(id)) return null;
                JsonObject product = products.getAsJsonObject(id);
                JsonObject quick = product.getAsJsonObject("quick_status");
                if (quick == null) return null;
                // Instant-buy price is the most useful "value" for a drop.
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
                    for (String key : new String[]{"price", "lowest", "starting_bid", "bin"}) {
                        if (obj.has(key) && obj.get(key).isJsonPrimitive()) return obj.get(key).getAsDouble();
                    }
                }
                return Double.parseDouble(value.replaceAll("[^0-9.\\-]", ""));
            } catch (Exception ignored) { return null; }
        }

        private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    }
}
