package com.skyblockextras.rng;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Pure RNG chat-message matching logic.
 *
 * This class intentionally has no Minecraft dependencies, which means it can
 * be tested quickly with Gradle without starting Minecraft.
 */
public final class RngMessageMatcher {

    private static final Pattern DROP_MESSAGE = Pattern.compile(
            "(?i)(RARE DROP|PRAY RNGESUS|CRAZY RARE|PET DROP|DROP)"
    );

    private RngMessageMatcher() {
    }

    public static String findDrop(String message, Collection<String> trackableDrops) {
        if (message == null || message.isBlank() || trackableDrops == null || trackableDrops.isEmpty()) {
            return null;
        }

        String normalized = stripMinecraftFormatting(message).trim();

        if (!DROP_MESSAGE.matcher(normalized).find()) {
            return null;
        }

        for (String item : trackableDrops) {
            if (containsWholePhrase(normalized, item)) {
                return item;
            }
        }

        return null;
    }

    public static boolean containsWholePhrase(String text, String phrase) {
        if (text == null || phrase == null || phrase.isBlank()) {
            return false;
        }

        String regex = "(?i)(?<![A-Za-z0-9_])"
                + Pattern.quote(phrase.trim())
                + "(?![A-Za-z0-9_])";

        return Pattern.compile(regex).matcher(text).find();
    }

    public static String stripMinecraftFormatting(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("§.", "")
                .replaceAll("\\u00A7.", "")
                .trim();
    }
}
