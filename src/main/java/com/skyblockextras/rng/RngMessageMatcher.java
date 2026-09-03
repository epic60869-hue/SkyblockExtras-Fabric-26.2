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

        // Farming drops must be announced with the Hypixel RARE CROP! prefix.
        // This prevents normal chat or pasted item names from triggering RNG timers.
        for (String item : trackableDrops) {
            if (isRareCropAnnouncement(normalized, item)) return item;
        }
        return null;
    }

    public static boolean isRareCropAnnouncement(String text, String item) {
        if (text == null || item == null || item.isBlank()) return false;
        String regex = "(?i)^\\s*RARE\\s+CROP!\\s*" + Pattern.quote(item.trim()) + "\\s*[.!]?\\s*$";
        return Pattern.compile(regex).matcher(text).find();
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
