package com.skyblockextras.pet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the pet overlay synchronized with Hypixel's live TAB widget.
 *
 * Hypixel's pet widget is not guaranteed to be stored in the local player's
 * PlayerInfo display name. It can be rendered as a separate TAB entry, so the
 * overlay must scan the whole listed-player collection.
 */
public final class PetTabSync {
    private static final Pattern PET_LINE = Pattern.compile(
            "(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(?:(\\d+)\\s*[♦◆✦])?\\s*(.+)"
    );

    private static final String[] KNOWN_PETS = {
            "Alligator", "Ammonite", "Ankylosaurus", "Armadillo", "Baby Yeti", "Bal", "Bat", "Bee", "Black Cat",
            "Blaze", "Blue Whale", "Chicken", "Dolphin", "Elephant", "Ender Dragon", "Enderman", "Endermite",
            "Flying Fish", "Giraffe", "Golden Dragon", "Golem", "Glacite Golem", "Grandma Wolf", "Griffin", "Guardian",
            "Hedgehog", "Horse", "Hound", "Jade Dragon", "Jellyfish", "Jerry", "Kuudra", "Lion", "Magma Cube",
            "Megalodon", "Mithril Golem", "Mole", "Monkey", "Mooshroom Cow", "Mosquito", "Ocelot", "Parrot",
            "Phoenix", "Pig", "Pigman", "Rabbit", "Rat", "Reindeer", "Rock", "Rose Dragon", "Scatha", "Sheep",
            "Silverfish", "Skeleton", "Skeleton Horse", "Slug", "Snail", "Snowman", "Spirit", "Squid", "Tarantula",
            "Tiger", "Turtle", "Wisp", "Wither Skeleton", "Wolf", "Zombie", "Rift Ferret"
    };

    private final PetOverlay overlay;
    private String lastPet = "";

    public PetTabSync(PetOverlay overlay) {
        this.overlay = overlay;
    }

    public void tick(Minecraft client) {
        if (overlay == null || client == null || client.player == null || client.getConnection() == null) return;

        Collection<PlayerInfo> players = client.getConnection().getListedOnlinePlayers();
        if (players == null || players.isEmpty()) return;

        String pendingPetLabel = null;

        for (PlayerInfo info : players) {
            if (info == null) continue;
            Component display = info.getTabListDisplayName();
            if (display == null) continue;

            String text = strip(display.getString());
            if (text.isBlank()) continue;

            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("pet:")) {
                String after = text.substring(lower.indexOf("pet:") + 4).trim();
                ParsedPet parsed = parsePet(after);
                if (parsed != null) {
                    apply(parsed);
                    return;
                }
                pendingPetLabel = "";
                continue;
            }

            if (pendingPetLabel != null) {
                ParsedPet parsed = parsePet(text);
                if (parsed != null) {
                    apply(parsed);
                    return;
                }
                // Keep looking for the pet line for a few TAB entries rather
                // than assuming the next entry is always the pet immediately.
            }

            // Also support a single TAB entry where the Pet label is omitted.
            // Only accept a line that contains a known pet name and [Lvl].
            ParsedPet parsed = parsePet(text);
            if (parsed != null && (lower.contains("pet") || lower.contains("lvl"))) {
                apply(parsed);
                return;
            }
        }
    }

    private ParsedPet parsePet(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = PET_LINE.matcher(text);
        if (!matcher.find()) return null;

        int level;
        try {
            level = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }

        String details = matcher.group(3).trim();
        String pet = findKnownPet(details);
        if (pet == null) return null;

        String rarity = findRarity(details);
        return new ParsedPet(pet, rarity, level);
    }

    private void apply(ParsedPet parsed) {
        boolean changed = !parsed.name.equalsIgnoreCase(lastPet);
        if (!changed && parsed.name.equalsIgnoreCase(readPetName())) return;

        try {
            Method setPet = PetOverlay.class.getDeclaredMethod("setPet", String.class, String.class, int.class);
            setPet.setAccessible(true);
            setPet.invoke(overlay, parsed.name, parsed.rarity, parsed.level);

            if (changed) {
                setField("currentXp", 0L);
                setField("tabProgress", -1.0f);
                setField("petItem", "");
                setField("resolvedPetIcon", net.minecraft.world.item.ItemStack.EMPTY);
                setField("resolvedIconKey", "");
            }

            lastPet = parsed.name;
        } catch (ReflectiveOperationException ignored) {
            // Keep the existing PetOverlay detection as a fallback.
        }
    }

    private String readPetName() {
        try {
            Field field = PetOverlay.class.getDeclaredField("petName");
            field.setAccessible(true);
            Object value = field.get(overlay);
            return value instanceof String ? (String) value : "";
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private void setField(String name, Object value) throws ReflectiveOperationException {
        Field field = PetOverlay.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(overlay, value);
    }

    private static String findKnownPet(String text) {
        String cleaned = strip(text).replaceAll("^[✦✧★☆]+\\s*", "").trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String pet : KNOWN_PETS) {
            String target = pet.toLowerCase(Locale.ROOT);
            if (lower.equals(target)
                    || lower.startsWith(target + " ")
                    || lower.endsWith(" " + target)
                    || lower.contains(" " + target + " ")) {
                return pet;
            }
        }
        return null;
    }

    private static String findRarity(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String rarity : new String[]{"Mythic", "Divine", "Legendary", "Epic", "Rare", "Uncommon", "Common"}) {
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

    private record ParsedPet(String name, String rarity, int level) {}
}
