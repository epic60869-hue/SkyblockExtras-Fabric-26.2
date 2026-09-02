package com.skyblockextras.rng;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.text.Text;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RngTracker {
    private final SbeConfig config;
    private final Map<String, Long> lastDrops = new LinkedHashMap<>();

    /*
     * IMPORTANT:
     * This intentionally does NOT use a generic "RARE DROP" detector.
     * Add only confirmed item names to the lists in SbeConfig.
     */
    private final Map<String, String> exactDrops = new LinkedHashMap<>();

    public RngTracker(SbeConfig config) {
        this.config = config;
        rebuild();
    }

    private void rebuild() {
        exactDrops.clear();
        if (config.slugEnabled) {
            if (config.epicSlug) exactDrops.put("Epic Slug", "Epic Slug");
            if (config.legendarySlug) exactDrops.put("Legendary Slug", "Legendary Slug");
        }
        if (config.harvestFeastEnabled) {
            config.harvestFeastDrops.forEach((name, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) exactDrops.put(name, name);
            });
        }
        if (config.dyesEnabled) {
            config.farmingDyes.forEach((name, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) exactDrops.put(name, name);
            });
        }
    }

    public void handle(Text message) {
        if (!config.farmingRngEnabled) return;
        rebuild();

        String raw = message.getString();
        String normalized = raw.replaceAll("\u00A7.", "").trim();

        // Only count messages that look like an actual drop announcement.
        boolean dropMessage = Pattern.compile("(?i)(RARE DROP|PRAY RNGESUS|CRAZY RARE|PET DROP|DROP)").matcher(normalized).find();
        if (!dropMessage) return;

        for (String item : exactDrops.keySet()) {
            if (containsWholePhrase(normalized, item)) {
                long now = System.currentTimeMillis();
                Long previous = lastDrops.put(item, now);
                config.save();

                String elapsed = previous == null ? "first tracked drop" : format(now - previous);
                if (SkyblockExtrasClient.class != null) {
                    var player = net.minecraft.client.Minecraft.getInstance().player;
                    if (player != null) {
                        player.sendMessage(Text.literal("[SBE] " + item + " — " + elapsed + " since last " + item), false);
                    }
                }
                break;
            }
        }
    }

    private boolean containsWholePhrase(String text, String phrase) {
        return text.toLowerCase().contains(phrase.toLowerCase());
    }

    private String format(long ms) {
        Duration d = Duration.ofMillis(ms);
        long days = d.toDays();
        long hours = d.toHoursPart();
        long mins = d.toMinutesPart();
        long secs = d.toSecondsPart();
        if (days > 0) return String.format("%dd %02dh %02dm %02ds", days, hours, mins, secs);
        return String.format("%02dh %02dm %02ds", hours, mins, secs);
    }
}
