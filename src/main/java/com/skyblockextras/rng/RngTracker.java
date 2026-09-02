package com.skyblockextras.rng;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RngTracker {

    private final SbeConfig config;

    private final Map<String, Long> lastDrops = new LinkedHashMap<>();

    /*
     * IMPORTANT:
     * This intentionally does NOT use a generic "RARE DROP" detector.
     * Only items enabled in SbeConfig are tracked.
     */
    private final Map<String, String> exactDrops = new LinkedHashMap<>();

    public RngTracker(SbeConfig config) {
        this.config = config;
        rebuild();
    }

    private void rebuild() {
        exactDrops.clear();

        // Slugs
        if (config.slugEnabled) {
            if (config.epicSlug) {
                exactDrops.put("Epic Slug", "Epic Slug");
            }

            if (config.legendarySlug) {
                exactDrops.put("Legendary Slug", "Legendary Slug");
            }
        }

        // Harvest Feast
        if (config.harvestFeastEnabled && config.harvestFeastDrops != null) {
            config.harvestFeastDrops.forEach((name, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) {
                    exactDrops.put(name, name);
                }
            });
        }

        // Farming dyes
        if (config.dyesEnabled && config.farmingDyes != null) {
            config.farmingDyes.forEach((name, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) {
                    exactDrops.put(name, name);
                }
            });
        }
    }

    /**
     * Handles a Minecraft chat/game message.
     */
    public void handle(Component message) {
        if (message == null) {
            return;
        }

        if (!config.farmingRngEnabled) {
            return;
        }

        rebuild();

        String raw = message.getString();

        if (raw == null || raw.isBlank()) {
            return;
        }

        String normalized = stripMinecraftFormatting(raw).trim();

        /*
         * Only process messages which resemble a drop announcement.
         *
         * We still require the actual configured item name below,
         * so random chat messages containing unrelated words won't
         * create RNG entries.
         */
        boolean dropMessage = Pattern.compile(
                "(?i)(RARE DROP|PRAY RNGESUS|CRAZY RARE|PET DROP|DROP)"
        ).matcher(normalized).find();

        if (!dropMessage) {
            return;
        }

        for (String item : exactDrops.keySet()) {

            if (!containsWholePhrase(normalized, item)) {
                continue;
            }

            recordDrop(item);
            break;
        }
    }

    private void recordDrop(String item) {

        long now = System.currentTimeMillis();

        Long previous = lastDrops.put(item, now);

        /*
         * Save the timestamp so it can eventually survive
         * Minecraft restarts/relogins.
         */
        config.setLastDrop(item, now);
        config.save();

        String elapsed;

        if (previous == null) {
            elapsed = "first tracked drop";
        } else {
            elapsed = format(now - previous);
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {

            String message =
                    "[SBE] "
                    + item
                    + " — "
                    + elapsed
                    + " since last "
                    + item;

            client.player.sendSystemMessage(
                    Component.literal(message)
            );
        }
    }

    /**
     * Prevents matching "Epic Slug" inside a larger word.
     */
    private boolean containsWholePhrase(String text, String phrase) {

        if (text == null || phrase == null || phrase.isBlank()) {
            return false;
        }

        String regex =
                "(?i)(?<![A-Za-z0-9_])"
                + Pattern.quote(phrase)
                + "(?![A-Za-z0-9_])";

        return Pattern.compile(regex).matcher(text).find();
    }

    /**
     * Removes Minecraft legacy formatting codes.
     */
    private String stripMinecraftFormatting(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replaceAll("§.", "")
                .replaceAll("\\u00A7.", "")
                .trim();
    }

    private String format(long milliseconds) {

        Duration duration = Duration.ofMillis(milliseconds);

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format(
                    "%dd %02dh %02dm %02ds",
                    days,
                    hours,
                    minutes,
                    seconds
            );
        }

        return String.format(
                "%02dh %02dm %02ds",
                hours,
                minutes,
                seconds
        );
    }

    /**
     * Allows other parts of the mod to retrieve the last
     * time a particular RNG happened.
     */
    public Long getLastDrop(String item) {

        Long timestamp = lastDrops.get(item);

        if (timestamp != null) {
            return timestamp;
        }

        return config.getLastDrop(item);
    }

    /**
     * Clears a single RNG timer.
     */
    public void clearTimer(String item) {
        lastDrops.remove(item);
        config.removeLastDrop(item);
        config.save();
    }

    /**
     * Clears every RNG timer.
     */
    public void clearAllTimers() {
        lastDrops.clear();
        config.clearLastDrops();
        config.save();
    }
}
