package com.skyblockextras.rng;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Clean center-screen RNG announcement overlay. */
public class RngDropOverlay {
    private final SbeConfig config;
    private final PriceService prices = new PriceService();

    private String item = "";
    private String price = "Looking up price...";
    private long expiresAt = 0L;
    private long sequence = 0L;

    public RngDropOverlay(SbeConfig config) {
        this.config = config;
    }

    public void show(String itemName) {
        if (!config.rngDropOverlayEnabled || itemName == null || itemName.isBlank()) return;
        item = itemName;
        price = "Looking up price...";
        expiresAt = System.currentTimeMillis() + 5000L;
        long requestId = ++sequence;

        prices.lookup(itemName).thenAccept(result -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (requestId != sequence) return;
                price = result;
            });
        });
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.rngDropOverlayEnabled || item.isBlank() || System.currentTimeMillis() >= expiresAt) return;

        Minecraft client = Minecraft.getInstance();
        float scale = Math.max(0.5f, Math.min(3.0f, config.rngDropOverlayScale));
        String priceText = "(" + price + ")";
        int itemWidth = client.font.width(item);
        int priceWidth = client.font.width(priceText);
        int width = Math.max(190, Math.max(itemWidth, priceWidth) + 50);
        int height = 70;
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
            graphics.fill(0, 0, width, height, 0xE6111218);
            graphics.outline(0, 0, width, height, 0xFF41434E);
            graphics.fill(1, 1, width - 1, 3, 0xFFB96BFF);
        }

        String title = "RNG DROP!";
        graphics.text(client.font, Component.literal(title), (width - client.font.width(title)) / 2, 10, 0xFFC77DFF, true);
        graphics.text(client.font, Component.literal(item), (width - itemWidth) / 2, 29, 0xFFFFFFFF, true);
        graphics.text(client.font, Component.literal(priceText), (width - priceWidth) / 2, 47, 0xFFFFD45A, true);
        pose.popMatrix();
    }

    public boolean isVisible() {
        return config.rngDropOverlayEnabled && !item.isBlank() && System.currentTimeMillis() < expiresAt;
    }

    public String getItem() { return item; }
    public String getPrice() { return price; }

    private final class PriceService {
        private final HttpClient client = HttpClient.newBuilder().build();
        private final AtomicLong resourceRefresh = new AtomicLong(0L);
        private volatile String resourceItems = "";
        private volatile long resourceFetchedAt = 0L;

        CompletableFuture<String> lookup(String displayName) {
            return bazaarPrice(displayName).thenCompose(bazaar -> {
                if (bazaar != null) return CompletableFuture.completedFuture(bazaar);
                return auctionHousePrice(displayName);
            });
        }

        private CompletableFuture<String> bazaarPrice(String displayName) {
            return ensureResources().thenCompose(v -> {
                String id = findItemId(displayName, resourceItems);
                if (id == null) return CompletableFuture.completedFuture(null);
                return get("https://api.hypixel.net/v2/skyblock/bazaar")
                        .thenApply(body -> parseBazaar(body, id));
            });
        }

        private CompletableFuture<String> auctionHousePrice(String displayName) {
            return get("https://api.hypixel.net/v2/skyblock/auctions")
                    .thenApply(body -> parseLowestBin(body, displayName));
        }

        private CompletableFuture<Void> ensureResources() {
            long now = System.currentTimeMillis();
            if (!resourceItems.isBlank() && now - resourceFetchedAt < 6 * 60 * 60 * 1000L) {
                return CompletableFuture.completedFuture(null);
            }
            long marker = resourceRefresh.incrementAndGet();
            return get("https://api.hypixel.net/v2/resources/skyblock/items")
                    .thenAccept(body -> {
                        if (marker == resourceRefresh.get()) {
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
            String target = normalize(name);
            Pattern p = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[\\s\\S]{0,500}\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
            Matcher m = p.matcher(json);
            while (m.find()) if (normalize(m.group(2)).equals(target)) return m.group(1);
            return null;
        }

        private String parseBazaar(String json, String id) {
            if (json == null || json.isBlank()) return null;
            String needle = "\"" + id + "\"";
            int at = json.indexOf(needle);
            if (at < 0) return null;
            int end = Math.min(json.length(), at + 5000);
            String block = json.substring(at, end);
            Matcher m = Pattern.compile("\\\"sellPrice\\\"\\s*:\\s*([0-9.]+)").matcher(block);
            if (!m.find()) return null;
            return formatPrice(Double.parseDouble(m.group(1)));
        }

        private String parseLowestBin(String json, String displayName) {
            if (json == null || json.isBlank()) return "Price unavailable";
            String target = normalize(displayName);
            Matcher m = Pattern.compile("\\\"item_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[\\s\\S]{0,500}\\\"bin\\\"\\s*:\\s*(true|false)[\\s\\S]{0,300}\\\"starting_bid\\\"\\s*:\\s*(\\d+)").matcher(json);
            long lowest = Long.MAX_VALUE;
            while (m.find()) {
                if (Boolean.parseBoolean(m.group(2)) && normalize(m.group(1)).equals(target)) {
                    lowest = Math.min(lowest, Long.parseLong(m.group(3)));
                }
            }
            return lowest == Long.MAX_VALUE ? "Price unavailable" : formatPrice(lowest);
        }

        private String normalize(String value) {
            return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }

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
    }
}
