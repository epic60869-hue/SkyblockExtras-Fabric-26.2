package com.skyblockextras.pet;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the pet HUD in sync when Hypixel changes the pet through a Loadout. */
public final class PetLoadoutSync {
    private static final Pattern PET_NAME = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(.+)");
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
    private int tickCounter;
    private String lastDetectedPet = "";

    public PetLoadoutSync(PetOverlay overlay) {
        this.overlay = overlay;
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return;
        if (++tickCounter % 2 != 0) return;

        double radius = 4.5D;
        var box = client.player.getBoundingBox().inflate(radius);
        Candidate best = null;
        String current = readString("petName", "");
        boolean currentPetSeen = false;

        for (Entity entity : client.level.getEntities((Entity) null, box, Entity::isAlive)) {
            if (!(entity instanceof LivingEntity living) || living == client.player) continue;
            if (living.getCustomName() == null) continue;

            String text = strip(living.getCustomName().getString());
            Matcher matcher = PET_NAME.matcher(text);
            if (!matcher.find()) continue;

            int level;
            try {
                level = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }

            String pet = findKnownPet(matcher.group(2));
            if (pet == null) continue;

            double distance = living.distanceToSqr(client.player);
            if (!current.isBlank() && pet.equalsIgnoreCase(current)) currentPetSeen = true;
            if (best == null || distance < best.distance) best = new Candidate(pet, level, distance);
        }

        if (best == null) return;
        if (best.pet.equalsIgnoreCase(lastDetectedPet)) return;
        if (!current.isBlank() && best.pet.equalsIgnoreCase(current)) {
            lastDetectedPet = best.pet;
            return;
        }

        // Do not steal the HUD from the player's current pet just because another
        // player's pet is nearby. A real loadout swap removes the old pet first.
        if (!current.isBlank() && currentPetSeen) return;

        applyPet(best.pet, best.level);
        lastDetectedPet = best.pet;
    }

    /** Handles normal summon and Autopet chat notices for immediate pet changes. */
    public void handleChatMessage(String message) {
        if (message == null || message.isBlank()) return;
        String text = strip(message);
        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.startsWith("you summoned your ")) {
            applyChatPet(text.substring("You summoned your ".length()).replaceAll("[.!]$", "").trim(), -1);
        } else if (lower.startsWith("autopet equipped your ")) {
            String petText = text.substring("Autopet equipped your ".length()).replaceAll("\\s*!.*$", "").trim();
            int level = -1;
            Matcher matcher = PET_NAME.matcher(petText);
            if (matcher.find()) {
                try { level = Integer.parseInt(matcher.group(1)); } catch (NumberFormatException ignored) { }
                petText = matcher.group(2).trim();
            }
            applyChatPet(petText, level);
        }
    }

    private void applyChatPet(String text, int level) {
        String pet = findKnownPet(text);
        if (pet == null) return;
        applyPet(pet, level);
        lastDetectedPet = pet;
    }

    private void applyPet(String pet, int level) {
        try {
            Field nameField = field("petName");
            Field rarityField = field("petRarity");
            Field levelField = field("petLevel");
            Field xpField = field("currentXp");
            Field progressField = field("tabProgress");
            Field itemField = field("petItem");
            Field iconField = field("resolvedPetIcon");
            Field iconKeyField = field("resolvedIconKey");
            Field cacheField = field("petCache");

            String key = pet.toLowerCase(Locale.ROOT).trim();
            Object cache = ((java.util.Map<?, ?>) cacheField.get(overlay)).get(key);

            nameField.set(overlay, pet);
            levelField.setInt(overlay, level > 0 ? Math.min(200, Math.max(1, level)) : 1);
            progressField.setFloat(overlay, -1.0f);
            itemField.set(overlay, "");
            xpField.setLong(overlay, 0L);
            iconField.set(overlay, net.minecraft.world.item.ItemStack.EMPTY);
            iconKeyField.set(overlay, "");
            rarityField.set(overlay, "");

            if (cache != null) {
                Field cachedRarity = cache.getClass().getDeclaredField("rarity");
                Field cachedLevel = cache.getClass().getDeclaredField("level");
                Field cachedXp = cache.getClass().getDeclaredField("menuTotalXp");
                Field cachedItem = cache.getClass().getDeclaredField("petItem");
                Field cachedIcon = cache.getClass().getDeclaredField("icon");
                cachedRarity.setAccessible(true);
                cachedLevel.setAccessible(true);
                cachedXp.setAccessible(true);
                cachedItem.setAccessible(true);
                cachedIcon.setAccessible(true);

                String rarity = (String) cachedRarity.get(cache);
                int cachedLvl = cachedLevel.getInt(cache);
                long cachedTotalXp = cachedXp.getLong(cache);
                String held = (String) cachedItem.get(cache);
                net.minecraft.world.item.ItemStack icon = (net.minecraft.world.item.ItemStack) cachedIcon.get(cache);

                if (rarity != null) rarityField.set(overlay, rarity);
                if (level <= 0 && cachedLvl > 0) levelField.setInt(overlay, cachedLvl);
                if (cachedTotalXp >= 0L) xpField.setLong(overlay, cachedTotalXp);
                if (held != null) itemField.set(overlay, held);
                if (icon != null && !icon.isEmpty()) iconField.set(overlay, icon.copy());
            }
        } catch (ReflectiveOperationException ignored) {
            // Normal TAB/menu tracking remains the fallback if internals change.
        }
    }

    private Field field(String name) throws NoSuchFieldException {
        Field field = PetOverlay.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private String readString(String name, String fallback) {
        try {
            Object value = field(name).get(overlay);
            return value instanceof String ? (String) value : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static String findKnownPet(String text) {
        String cleaned = strip(text);
        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String pet : KNOWN_PETS) {
            String target = pet.toLowerCase(Locale.ROOT);
            if (lower.equals(target) || lower.startsWith(target + " ") || lower.endsWith(" " + target)
                    || lower.contains(" " + target + " ")) return pet;
        }
        return null;
    }

    private static String strip(String text) {
        return text == null ? "" : text.replaceAll("§[0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim();
    }

    private record Candidate(String pet, int level, double distance) { }
}
