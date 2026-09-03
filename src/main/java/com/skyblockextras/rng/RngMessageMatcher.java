package com.skyblockextras.rng;

import java.util.Collection;
import java.util.regex.Pattern;

/** Pure RNG chat-message matching logic. */
public final class RngMessageMatcher {
    private RngMessageMatcher() {
    }

    public static String findDrop(String message, Collection<String> trackableDrops) {
        if (message == null || message.isBlank() || trackableDrops == null || trackableDrops.isEmpty()) {
            return null;
        }

        String normalized = stripMinecraftFormatting(message).trim();
        if (normalized.isBlank()) return null;

        // Match the exact item name anywhere in the message. Harvest Feast
        // drops are validated separately by RngTracker so they still require
        // the real "RARE CROP!" announcement.
        for (String item : trackableDrops) {
            if (containsWholePhrase(normalized, item)) return item;
        }
        return null;
    }

    /**
     * Matches Hypixel's Harvest Feast announcement while allowing the dynamic
     * RNG reward suffix, e.g. "RARE CROP! Cropie (+106.8)".
     */
    public static boolean isRareCropAnnouncement(String text, String item) {
        if (text == null || item == null || item.isBlank()) return false;
        String regex = "(?i)^\\s*RARE\\s+CROP!\\s*" + Pattern.quote(item.trim())
                + "(?:\\s*\\(\\s*[+-]?[0-9,.]+(?:[kmb])?\\s*[^)]*\\))?\\s*[.!]?\\s*$";
        return Pattern.compile(regex).matcher(stripMinecraftFormatting(text)).matches();
    }

    public static boolean containsWholePhrase(String text, String phrase) {
        if (text == null || phrase == null || phrase.isBlank()) return false;
        String regex = "(?i)(?<![A-Za-z0-9_])" + Pattern.quote(phrase.trim()) + "(?![A-Za-z0-9_])";
        return Pattern.compile(regex).matcher(text).find();
    }

    public static String stripMinecraftFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("§.", "").replaceAll("\\u00A7.", "").trim();
    }
}
