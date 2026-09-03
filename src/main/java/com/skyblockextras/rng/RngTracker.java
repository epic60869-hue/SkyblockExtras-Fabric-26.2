package com.skyblockextras.rng;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class RngTracker {
    private static final long DUPLICATE_WINDOW_MS = 1500L;
    private static final Gson GSON = new Gson();

    private final SbeConfig config;
    private final Map<String, Long> lastDrops = new LinkedHashMap<>();
    private final Map<String, String> exactDrops = new LinkedHashMap<>();
    private final Map<String, Long> recentlyHandled = new LinkedHashMap<>();

    public RngTracker(SbeConfig config) {
        this.config = config;
        if (config.lastDrops != null) lastDrops.putAll(config.lastDrops);
        loadDrops();
    }

    private void loadDrops() {
        exactDrops.clear();
        try (InputStream stream = RngTracker.class.getClassLoader().getResourceAsStream("rng_drops.json")) {
            if (stream == null) {
                System.err.println("[SBE RNG] Could not find rng_drops.json");
                return;
            }
            JsonObject root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            JsonObject farming = getObject(root, "farming");
            if (farming != null) {
                addArray(farming, "harvestFeast");
                addArray(farming, "farmingDyes");
                addArray(farming, "slugs");
            }
            addCategory(root, "mining");
            addCategory(root, "fishing");
            addCategory(root, "combat");
            addCategory(root, "other");
            System.out.println("[SBE RNG] Loaded " + exactDrops.size() + " trackable RNG drops.");
        } catch (Exception e) {
            System.err.println("[SBE RNG] Failed to load rng_drops.json:");
            e.printStackTrace();
        }
    }

    private void addCategory(JsonObject root, String categoryName) {
        JsonObject category = getObject(root, categoryName);
        if (category == null) return;
        addArray(category, "rareDrops");
        addArray(category, "dyes");
    }

    private JsonObject getObject(JsonObject parent, String name) {
        if (!parent.has(name)) return null;
        JsonElement element = parent.get(name);
        if (element == null || !element.isJsonObject()) return null;
        return element.getAsJsonObject();
    }

    private void addArray(JsonObject parent, String name) {
        if (!parent.has(name)) return;
        JsonElement element = parent.get(name);
        if (element == null || !element.isJsonArray()) return;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry == null || !entry.isJsonPrimitive()) continue;
            String item = entry.getAsString();
            if (item != null && !item.isBlank()) exactDrops.put(item.trim(), item.trim());
        }
    }

    public void handle(Component message) {
        if (message == null || !config.farmingRngEnabled) return;

        String rawMessage = RngMessageMatcher.stripMinecraftFormatting(message.getString());
        String item = RngMessageMatcher.findDrop(rawMessage, exactDrops.keySet());
        if (item == null || !isEnabled(item)) return;

        // Harvest Feast drops must use Hypixel's exact RARE CROP! announcement.
        // This prevents normal chat/item-name mentions from triggering the RNG.
        if (isHarvestFeastDrop(item) && !RngMessageMatcher.containsWholePhrase(rawMessage, "RARE CROP!")) {
            return;
        }

        recordDrop(item);
    }

    private boolean isHarvestFeastDrop(String item) {
        return config.harvestFeastDrops != null && config.harvestFeastDrops.containsKey(item);
    }

    private boolean isEnabled(String item) {
        if (item == null || item.isBlank()) return false;
        if (item.equalsIgnoreCase("Epic Slug")) return config.slugEnabled && config.epicSlug;
        if (item.equalsIgnoreCase("Legendary Slug")) return config.slugEnabled && config.legendarySlug;
        Boolean harvest = config.harvestFeastDrops.get(item);
        if (harvest != null) return config.harvestFeastEnabled && harvest;
        Boolean dye = config.farmingDyes.get(item);
        if (dye != null) return config.dyesEnabled && dye;
        return true;
    }

    private void recordDrop(String item) {
        long now = System.currentTimeMillis();
        Long recent = recentlyHandled.get(item);
        if (recent != null && now - recent < DUPLICATE_WINDOW_MS) return;
        recentlyHandled.put(item, now);
        recentlyHandled.entrySet().removeIf(entry -> now - entry.getValue() >= DUPLICATE_WINDOW_MS);

        Long previous = lastDrops.put(item, now);
        config.setLastDrop(item, now);
        config.save();

        String elapsed = previous == null ? "first tracked drop" : format(now - previous);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("[RNG] " + item + " — " + elapsed + " since last " + item));
        }
        if (SkyblockExtrasClient.RNG_DROP_OVERLAY != null) {
            SkyblockExtrasClient.RNG_DROP_OVERLAY.show(item);
        }
        System.out.println("[SBE RNG] " + item + " detected.");
    }

    private String format(long milliseconds) {
        Duration duration = Duration.ofMillis(milliseconds);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (days > 0) return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public Long getLastDrop(String item) {
        Long timestamp = lastDrops.get(item);
        return timestamp != null ? timestamp : config.getLastDrop(item);
    }

    public void clearTimer(String item) {
        lastDrops.remove(item);
        config.removeLastDrop(item);
        config.save();
    }

    public void clearAllTimers() {
        lastDrops.clear();
        config.clearLastDrops();
        config.save();
    }
}
