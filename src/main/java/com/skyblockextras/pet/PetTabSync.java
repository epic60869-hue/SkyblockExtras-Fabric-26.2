package com.skyblockextras.pet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the active pet directly from Hypixel's TAB "Pet:" section.
 *
 * We deliberately do not use a hard-coded pet list. Whatever text Hypixel
 * places after [Lvl X] in the Pet section becomes the pet name.
 */
public final class PetTabSync {
    private static final Pattern PET_LINE = Pattern.compile(
            "(?i)^\\[?lvl\\s*(\\d+)\\]?\\s*(?:\\d+\\s*[♦◆✦]\\s*)?(.+?)\\s*$"
    );

    private final PetOverlay overlay;
    private String lastPetLine = "";

    public PetTabSync(PetOverlay overlay) {
        this.overlay = overlay;
    }

    public void tick(Minecraft client) {
        if (overlay == null || client == null || client.player == null || client.getConnection() == null) return;

        // Hypixel can use TAB entries which are not marked as "listed".
        // getListedOnlinePlayers() therefore isn't sufficient for the custom
        // SkyBlock TAB widget. Try the complete online-player collection first.
        Collection<PlayerInfo> players = getAllPlayers(client);
        if (players == null || players.isEmpty()) return;

        List<String> lines = new ArrayList<>();
        for (PlayerInfo info : players) {
            if (info == null) continue;
            Component display = info.getTabListDisplayName();
            if (display == null) continue;

            String text = strip(display.getString());
            if (text.isBlank()) continue;

            for (String line : text.split("\\R")) {
                line = strip(line);
                if (!line.isBlank()) lines.add(line);
            }
        }

        // Find the actual Pet: section and read the line immediately following
        // it. This means new pets automatically work without updating a list.
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).equalsIgnoreCase("Pet:")
                    && !lines.get(i).toLowerCase(Locale.ROOT).startsWith("pet:")) {
                continue;
            }

            String inline = "";
            String current = lines.get(i);
            if (current.length() > 4 && current.regionMatches(true, 0, "Pet:", 0, 4)) {
                inline = current.substring(4).trim();
            }

            // Usually the pet is on the next TAB line:
            // Pet:
            // [Lvl 169] 0♦ Rose Dragon
            if (inline.isBlank()) {
                for (int j = i + 1; j < lines.size(); j++) {
                    String candidate = lines.get(j);
                    ParsedPet parsed = parsePetLine(candidate);
                    if (parsed != null) {
                        apply(parsed);
                        return;
                    }

                    // Don't accidentally walk into a different TAB section.
                    if (isSectionLabel(candidate)) break;
                }
            } else {
                ParsedPet parsed = parsePetLine(inline);
                if (parsed != null) {
                    apply(parsed);
                    return;
                }
            }
        }
    }

    /**
     * Get all PlayerInfo entries, including entries Hypixel may mark as not
     * listed. Reflection keeps this compatible with the current mappings while
     * falling back to the normal listed-player API if necessary.
     */
    @SuppressWarnings("unchecked")
    private Collection<PlayerInfo> getAllPlayers(Minecraft client) {
        try {
            Method method = client.getConnection().getClass().getMethod("getOnlinePlayers");
            Object result = method.invoke(client.getConnection());
            if (result instanceof Collection<?>) {
                return (Collection<PlayerInfo>) result;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall back below.
        }

        return client.getConnection().getListedOnlinePlayers();
    }

    private ParsedPet parsePetLine(String line) {
        if (line == null || line.isBlank()) return null;

        Matcher matcher = PET_LINE.matcher(line);
        if (!matcher.matches()) return null;

        try {
            int level = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2).trim();
            if (name.isBlank()) return null;

            // Remove only the leading rarity/star decoration. Everything else
            // in the Pet section is intentionally left untouched.
            name = name.replaceFirst("^[✦✧★☆]+\\s*", "").trim();
            if (name.isBlank()) return null;

            return new ParsedPet(name, findRarity(line), level, line);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void apply(ParsedPet parsed) {
        // Don't repeatedly reset the overlay every tick when TAB hasn't changed.
        if (parsed.rawLine.equals(lastPetLine)) return;
        lastPetLine = parsed.rawLine;

        try {
            Field petName = PetOverlay.class.getDeclaredField("petName");
            petName.setAccessible(true);
            String oldPet = String.valueOf(petName.get(overlay));

            boolean changed = !parsed.name.equalsIgnoreCase(oldPet);

            Method setPet = PetOverlay.class.getDeclaredMethod(
                    "setPet", String.class, String.class, int.class
            );
            setPet.setAccessible(true);
            setPet.invoke(overlay, parsed.name, parsed.rarity, parsed.level);

            if (changed) {
                setField("currentXp", 0L);
                setField("tabProgress", -1.0f);
                setField("petItem", "");
                setField("resolvedPetIcon", net.minecraft.world.item.ItemStack.EMPTY);
                setField("resolvedIconKey", "");
            }
        } catch (ReflectiveOperationException ignored) {
            // PetOverlay's normal detection remains as a fallback.
        }
    }

    private void setField(String name, Object value) throws ReflectiveOperationException {
        Field field = PetOverlay.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(overlay, value);
    }

    private static boolean isSectionLabel(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.endsWith(":") && !lower.startsWith("[lvl");
    }

    private static String findRarity(String text) {
        String lower = strip(text).toLowerCase(Locale.ROOT);
        for (String rarity : new String[]{
                "Mythic", "Divine", "Legendary", "Epic", "Rare", "Uncommon", "Common"
        }) {
            if (lower.contains(rarity.toLowerCase(Locale.ROOT))) return rarity;
        }
        return "";
    }

    private static String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orx]", "")
                .replaceAll("\\u00A7[0-9a-fk-orx]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ParsedPet(String name, String rarity, int level, String rawLine) {}
}
