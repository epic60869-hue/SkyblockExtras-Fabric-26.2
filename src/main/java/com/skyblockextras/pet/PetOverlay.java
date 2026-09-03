package com.skyblockextras.pet;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SkyBlock pet HUD. The received Pet: TAB widget is the source of truth for the equipped pet. */
public class PetOverlay {
    private static final long LEGENDARY_LEVEL_100_XP = 25_353_230L;
    private static final long OVERFLOW_XP_PER_LEVEL = 1_886_700L;

    private final SbeConfig config;
    private String petName = "No Pet";
    private String petRarity = "";
    private int petLevel = 1;
    private long currentXp = 0L;
    private float tabProgress = -1.0f;
    private String petItem = "";
    private ItemStack resolvedPetIcon = ItemStack.EMPTY;
    private String resolvedIconKey = "";
    private final Map<String, PetCache> petCache = new HashMap<>();

    private static final Pattern PET_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(?:(\\d+)\\s*[♦◆✦])?\\s*(.+)");
    private static final Pattern PET_XP_LINE = Pattern.compile("(?i)([0-9,.]+(?:[kmb])?)\\s*/\\s*([0-9,.]+(?:[kmb])?)\\s*XP(?:\\s*\\(([0-9,.]+)%\\))?");
    private static final Pattern PET_LABEL_XP = Pattern.compile("(?i)pet\\s*xp\\s*[:：]\\s*([0-9,.]+(?:[kmb])?)(?:\\s*/\\s*([0-9,.]+(?:[kmb])?))?");
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)(?:held item|pet item)\\s*[:：]\\s*(.+)");
    private static final Pattern ENTITY_LEVEL_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?");
    private static final Pattern HELD_ITEM_LORE = Pattern.compile("(?i)^held item\\s*:\\s*(.+)$");

    private static final String ROSE_DRAGON_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc2MTE3MzAyMjM0NywKICAicHJvZmlsZUlkIiA6ICJjOWI3OWY2OGEyZGY0YjA1ODUwOWRlMzg5YjM5ZDUyYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ3cmVlcGVyX2Jvb3AiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI3YzNkZTA3NWEyYmIyMzhlZjUxNDMxMjA2YjEwZDU4NmNiMmE1YjFjYzQxZmU4NTFjYzVmMGIwMmQzNTdjNyIsCiAgICAgICAgIm1ldGFkYXRhIiA6IHsibW9kZWwiIDogInNsaW0ifQogICAgfQogIH0KfQ==";

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

    private static final int[] PET_XP = {
        100,110,120,130,145,160,175,190,210,230,250,275,300,330,360,400,440,490,540,600,
        660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,
        3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,
        23200,25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,
        90700,97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,
        272700,291700,311700,333700,357700,383700,411700,441700,476700,516700,561700,611700,666700,726700,791700,861700,
        936700,1016700,1101700,1191700,1286700,1386700,1496700,1616700,1746700,1886700
    };

    public PetOverlay(SbeConfig config) { this.config = config; }

    public void tick(Minecraft client) {
        if (!config.petOverlayEnabled || client == null || client.player == null) return;
        readHypixelTab(client);
        readOpenedPetsMenu(client);
        resolveRealPetIcon(client);
    }

    private void readHypixelTab(Minecraft client) {
        if (client.getConnection() == null || client.player == null) return;

        Collection<PlayerInfo> players = client.getConnection().getListedOnlinePlayers();
        PlayerInfo localInfo = null;
        StringBuilder localText = new StringBuilder();
        StringBuilder fallbackText = new StringBuilder();

        UUID localUuid = client.player.getUUID();
        for (PlayerInfo info : players) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;
            String text = display.getString();
            if (info.getProfile() != null && localUuid.equals(info.getProfile().id())) {
                localInfo = info;
                localText.append(text).append('\n');
            }
            fallbackText.append(text).append('\n');
        }

        if (localInfo != null) {
            String local = localText.toString();
            if (containsPetWidget(local)) {
                parseTabText(local);
                return;
            }
        }

        parseTabText(fallbackText.toString());
    }

    private static boolean containsPetWidget(String text) {
        if (text == null) return false;
        String lower = stripFormatting(text).toLowerCase(Locale.ROOT);
        return lower.contains("pet:") || lower.matches("(?s).*\\[lvl\\s*\\d+\\].*");
    }

    private void parseTabText(String raw) {
        if (raw == null || raw.isBlank()) return;
        String[] lines = raw.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = stripFormatting(lines[i]);
            if (line.equalsIgnoreCase("Pet:") || line.toLowerCase(Locale.ROOT).startsWith("pet:")) {
                String inline = line.length() > 4 ? line.substring(4).trim() : "";
                if (!inline.isBlank()) {
                    ParsedPet parsed = parsePetLine(inline);
                    if (parsed != null) {
                        applyTabPet(parsed, lines, i + 1);
                        return;
                    }
                }
                for (int j = i + 1; j < Math.min(lines.length, i + 6); j++) {
                    ParsedPet parsed = parsePetLine(stripFormatting(lines[j]));
                    if (parsed != null) {
                        applyTabPet(parsed, lines, j + 1);
                        return;
                    }
                }
            }
        }

        for (int i = 0; i < lines.length; i++) {
            ParsedPet parsed = parsePetLine(stripFormatting(lines[i]));
            if (parsed != null) {
                applyTabPet(parsed, lines, i + 1);
                return;
            }
        }
    }

    private ParsedPet parsePetLine(String line) {
        if (line == null || line.isBlank()) return null;
        Matcher pet = PET_PATTERN.matcher(line);
        if (!pet.find()) return null;
        try {
            int level = Integer.parseInt(pet.group(1));
            String details = pet.group(3).trim();
            String name = findPetName(details);
            if (name == null) return null;
            return new ParsedPet(name, findRarity(details), level);
        } catch (NumberFormatException ignored) { return null; }
    }

    private void applyTabPet(ParsedPet parsed, String[] lines, int startIndex) {
        String oldPet = petName;
        setPet(parsed.name, parsed.rarity, parsed.level);
        long localXp = -1L;
        float progress = -1.0f;
        String parsedItem = "";
        for (int i = startIndex; i < Math.min(lines.length, startIndex + 5); i++) {
            String line = stripFormatting(lines[i]);
            if (line.isBlank()) continue;
            Matcher xp = PET_XP_LINE.matcher(line);
            if (xp.find()) {
                localXp = parseNumber(xp.group(1));
                if (xp.group(3) != null) progress = parsePercent(xp.group(3));
                continue;
            }
            Matcher labelXp = PET_LABEL_XP.matcher(line);
            if (labelXp.find()) {
                localXp = parseNumber(labelXp.group(1));
                continue;
            }
            Matcher item = ITEM_PATTERN.matcher(line);
            if (item.find()) parsedItem = item.group(1).replaceAll("\\s+", " ").trim();
        }

        PetCache cached = petCache.get(cacheKey(petName));
        if (!parsedItem.isBlank()) petItem = parsedItem;
        else if (cached != null && !cached.petItem.isBlank()) petItem = cached.petItem;

        if (localXp >= 0) currentXp = calculateTotalXp(petLevel, localXp, petRarity);
        else if (cached != null && cached.menuTotalXp >= 0) currentXp = cached.menuTotalXp;

        if (progress >= 0) tabProgress = progress;

        if (!petName.equalsIgnoreCase(oldPet)) {
            PetCache newCache = petCache.get(cacheKey(petName));
            if (newCache != null) {
                if (!newCache.petItem.isBlank() && parsedItem.isBlank()) petItem = newCache.petItem;
                if (!newCache.icon.isEmpty()) {
                    resolvedPetIcon = newCache.icon.copy();
                    resolvedIconKey = "cache:" + cacheKey(petName);
                } else {
                    resolvedPetIcon = ItemStack.EMPTY;
                    resolvedIconKey = "";
                }
                if (newCache.menuTotalXp >= 0 && localXp < 0) currentXp = newCache.menuTotalXp;
            }
        }
    }

    private void readOpenedPetsMenu(Minecraft client) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        String title = stripFormatting(screen.getTitle().getString());
        if (!title.matches("(?:\\(\\d+/\\d+\\) )?Pets(?:.*)?")) return;

        for (var slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            PetMenuInfo info = readPetMenuInfo(stack);
            if (info == null || !(info.active || isActivePetByLore(stack))) continue;

            String menuPet = findPetName(info.type);
            if (menuPet == null) menuPet = info.type;
            setPet(menuPet, info.rarity, info.level);
            if (info.experience >= 0.0D) currentXp = (long) info.experience;

            String held = heldItemFromLore(stack);
            if (held.isBlank()) held = info.heldItem;
            if (held != null && !held.isBlank()) petItem = stripFormatting(held).trim();

            PetCache cached = petCache.computeIfAbsent(cacheKey(menuPet), k -> new PetCache());
            if (!petItem.isBlank()) cached.petItem = petItem;
            if (info.experience >= 0.0D) cached.menuTotalXp = (long) info.experience;
            cached.rarity = petRarity;
            cached.level = petLevel;

            if (stack.get(DataComponents.PROFILE) != null) {
                cached.icon = stack.copy();
                resolvedPetIcon = stack.copy();
                resolvedIconKey = "menu-profile:" + menuPet;
            }
            return;
        }
    }

    private PetMenuInfo readPetMenuInfo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        try {
            var root = data.copyTag();
            var attributes = root.getCompoundOrEmpty("ExtraAttributes");
            String raw = attributes.getStringOr("petInfo", root.getStringOr("petInfo", ""));
            if (raw.isBlank()) return null;
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (!json.has("type")) return null;
            String type = json.get("type").getAsString();
            String rarity = json.has("tier") && !json.get("tier").isJsonNull() ? json.get("tier").getAsString() : "";
            boolean active = json.has("active") && json.get("active").getAsBoolean();
            String heldItem = json.has("heldItem") && !json.get("heldItem").isJsonNull() ? json.get("heldItem").getAsString() : "";
            double experience = json.has("exp") ? json.get("exp").getAsDouble() : -1.0D;
            int level = experience >= 0.0D ? calculateLevelFromTotalXp((long) experience, rarity, type) : petLevel;
            return new PetMenuInfo(type, rarity, active, heldItem, experience, level);
        } catch (RuntimeException ignored) { return null; }
    }

    private boolean isActivePetByLore(ItemStack stack) {
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        return lore.lines().stream().anyMatch(line -> stripFormatting(line.getString()).contains("Click to despawn!"));
    }

    private String heldItemFromLore(ItemStack stack) {
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        for (Component line : lore.lines()) {
            Matcher matcher = HELD_ITEM_LORE.matcher(stripFormatting(line.getString()).trim());
            if (matcher.matches()) return matcher.group(1).trim();
        }
        return "";
    }

    private void resolveRealPetIcon(Minecraft client) {
        if (client.level == null || client.player == null || petName.equals("No Pet")) return;
        PetCache cached = petCache.get(cacheKey(petName));
        if (cached != null && !cached.icon.isEmpty()) {
            resolvedPetIcon = cached.icon.copy();
            resolvedIconKey = "cache:" + cacheKey(petName);
            return;
        }
        if (petName.equalsIgnoreCase("Rose Dragon")) {
            ItemStack rose = createTexturedHead(ROSE_DRAGON_TEXTURE, "Rose Dragon");
            if (!rose.isEmpty()) {
                resolvedPetIcon = rose;
                resolvedIconKey = "profile:rose_dragon";
                petCache.computeIfAbsent(cacheKey(petName), k -> new PetCache()).icon = rose.copy();
                return;
            }
        }

        LivingEntity namedAnchor = null;
        double anchorDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !living.isAlive()) continue;
            double distance = living.distanceToSqr(client.player);
            if (distance > 18.0D * 18.0D) continue;
            Component custom = living.getCustomName();
            if (custom == null) continue;
            String name = stripFormatting(custom.getString()).toLowerCase(Locale.ROOT);
            if (!name.contains(petName.toLowerCase(Locale.ROOT))) continue;
            if (name.contains("slug") && !petName.equalsIgnoreCase("Slug")) continue;
            if (distance < anchorDistance) { anchorDistance = distance; namedAnchor = living; }
        }
        if (namedAnchor != null) {
            ItemStack anchored = findHeadNear(client, namedAnchor, 3.0D);
            if (!anchored.isEmpty()) {
                cacheIcon(anchored);
                resolvedPetIcon = anchored;
                resolvedIconKey = "anchor:" + cacheKey(petName);
                return;
            }
        }

        double maxDistance = 12.0D;
        var box = client.player.getBoundingBox().inflate(maxDistance);
        Entity bestEntity = null;
        ItemStack bestStack = ItemStack.EMPTY;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Entity entity : client.level.getEntities((Entity) null, box, Entity::isAlive)) {
            ItemStack head = ItemStack.EMPTY;
            boolean armorStand = entity instanceof ArmorStand;
            if (armorStand) head = ((ArmorStand) entity).getItemBySlot(EquipmentSlot.HEAD);
            else if (entity instanceof Display.ItemDisplay) head = ((Display.ItemDisplay) entity).getItemStack();
            if (!isUsablePetHead(head)) continue;
            double distanceSq = entity.distanceToSqr(client.player);
            if (distanceSq > maxDistance * maxDistance) continue;
            double score = -distanceSq * 2.0D;
            if (entity instanceof LivingEntity living && living.getCustomName() != null) {
                String custom = stripFormatting(living.getCustomName().getString()).toLowerCase(Locale.ROOT);
                if (custom.contains(petName.toLowerCase(Locale.ROOT))) score += 2500.0D;
                if (custom.contains("slug") && !petName.equalsIgnoreCase("Slug")) score -= 5000.0D;
            }
            if (armorStand) {
                ArmorStand stand = (ArmorStand) entity;
                String customName = stand.getCustomName() == null ? "" : stripFormatting(stand.getCustomName().getString());
                Matcher levelMatcher = ENTITY_LEVEL_PATTERN.matcher(customName);
                if (levelMatcher.find()) {
                    int entityLevel = parseInt(levelMatcher.group(1));
                    if (entityLevel == petLevel) score += 1500.0D; else score -= 1000.0D;
                }
                if (stand.isInvisible()) score += 500.0D;
                if (stand.isMarker()) score += 350.0D;
                if (stand.isSmall()) score += 150.0D;
            }
            if (score > bestScore) { bestScore = score; bestEntity = entity; bestStack = head.copy(); }
        }
        if (bestEntity != null && !bestStack.isEmpty()) {
            cacheIcon(bestStack);
            resolvedPetIcon = bestStack;
            resolvedIconKey = "entity:" + cacheKey(petName);
        }
    }

    private void cacheIcon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        petCache.computeIfAbsent(cacheKey(petName), k -> new PetCache()).icon = stack.copy();
    }

    private ItemStack createTexturedHead(String textureValue, String name) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8));
            Property property = new Property("textures", textureValue);
            PropertyMap properties = new PropertyMap(ImmutableMultimap.<String, Property>builder().put("textures", property).build());
            GameProfile profile = new GameProfile(uuid, "SkyBlockPet", properties);
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            if (name != null && !name.isBlank()) stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            return stack;
        } catch (Throwable ignored) { return ItemStack.EMPTY; }
    }

    private ItemStack findHeadNear(Minecraft client, LivingEntity anchor, double radius) {
        double radiusSq = radius * radius;
        ItemStack best = ItemStack.EMPTY;
        double bestScore = Double.NEGATIVE_INFINITY;
        var box = anchor.getBoundingBox().inflate(radius);
        for (Entity entity : client.level.getEntities((Entity) null, box, Entity::isAlive)) {
            ItemStack head = ItemStack.EMPTY;
            if (entity instanceof ArmorStand stand) head = stand.getItemBySlot(EquipmentSlot.HEAD);
            else if (entity instanceof Display.ItemDisplay display) head = display.getItemStack();
            if (!isUsablePetHead(head)) continue;
            double distanceSq = entity.distanceToSqr(anchor);
            if (distanceSq > radiusSq) continue;
            double score = -distanceSq * 10.0D;
            if (entity instanceof ArmorStand stand) {
                if (stand.isInvisible()) score += 300.0D;
                if (stand.isMarker()) score += 250.0D;
                if (stand.isSmall()) score += 100.0D;
            } else score += 500.0D;
            if (score > bestScore) { bestScore = score; best = head.copy(); }
        }
        return best;
    }

    private static boolean isUsablePetHead(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.PLAYER_HEAD && stack.get(DataComponents.PROFILE) != null;
    }

    private long calculateTotalXp(int level, long localXp, String rarity) {
        if (level > 100) return LEGENDARY_LEVEL_100_XP + (long) (level - 100) * OVERFLOW_XP_PER_LEVEL + Math.max(0L, localXp);
        return calculateNormalPetTotalXp(level, localXp, rarity);
    }

    private long calculateNormalPetTotalXp(int level, long localXp, String rarity) {
        if (level <= 1) return Math.max(0L, localXp);
        long total = 0L;
        int completed = Math.min(level - 1, 99);
        int offset = getRarityOffset(rarity);
        for (int i = 0; i < completed; i++) {
            int index = offset + i;
            if (index >= PET_XP.length) break;
            total += PET_XP[index];
        }
        return total + Math.max(0L, localXp);
    }

    private static int getRarityOffset(String rarity) {
        return switch (rarity == null ? "" : rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 0;
            case "uncommon" -> 6;
            case "rare" -> 11;
            case "epic" -> 16;
            default -> 20;
        };
    }

    private static int calculateLevelFromTotalXp(long totalXp, String rarity, String type) {
        if (totalXp < LEGENDARY_LEVEL_100_XP) {
            long spent = 0L;
            int offset = getRarityOffset(rarity);
            for (int level = 1; level < 100; level++) {
                int index = offset + level - 1;
                long requirement = index < PET_XP.length ? PET_XP[index] : OVERFLOW_XP_PER_LEVEL;
                if (totalXp < spent + requirement) return level;
                spent += requirement;
            }
            return 100;
        }
        return 100 + (int) ((totalXp - LEGENDARY_LEVEL_100_XP) / OVERFLOW_XP_PER_LEVEL);
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!config.petOverlayEnabled || client.player == null) return;
        float scale = config.petScale <= 0 ? 1.0f : config.petScale;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(config.petX, config.petY);
        pose.scale(scale, scale);

        int textX = config.showPetIcon ? 20 : 0;
        boolean showXpLine = config.showPetXp || config.showPetProgress;
        int height = 12;
        if (showXpLine) height += 10;
        if (config.showPetItem && !petItem.isBlank()) height += 10;
        int width = getOverlayWidth(client, showXpLine);

        if (config.petBackgroundEnabled) {
            graphics.fill(textX - 4, -3, width, height + 3, 0xB9101117);
            graphics.outline(textX - 4, -3, width, height + 3, 0xFF41434E);
            graphics.fill(textX - 4, -3, textX - 1, height + 3, rarityColor());
        }
        if (config.showPetIcon && !resolvedPetIcon.isEmpty()) graphics.item(resolvedPetIcon, 0, 0);

        int y = 0;
        String levelText = config.showPetLevel ? "[Lvl " + displayLevel() + "] " : "";
        if (!levelText.isEmpty()) graphics.text(client.font, Component.literal(levelText), textX, y, 0xFFAAAAAA, true);
        int nameX = textX + (levelText.isEmpty() ? 0 : client.font.width(levelText));
        graphics.text(client.font, Component.literal(petName), nameX, y, rarityColor(), true);
        y += 12;

        if (showXpLine) {
            graphics.text(client.font, Component.literal("Pet XP: " + formatNumber(currentXp) + " - " + formatPercent(levelProgress())), textX, y, 0xFF55FFFF, true);
            y += 10;
        }
        if (config.showPetItem && !petItem.isBlank()) graphics.text(client.font, Component.literal("Pet Item: " + petItem), textX, y, 0xFFAA55FF, true);
        pose.popMatrix();
    }

    private int getOverlayWidth(Minecraft client, boolean showXpLine) {
        int width = client.font.width("[Lvl 999] ") + client.font.width(petName) + 24;
        if (showXpLine) width = Math.max(width, client.font.width("Pet XP: 999,999,999 - 100.0%") + 24);
        if (config.showPetItem && !petItem.isBlank()) width = Math.max(width, client.font.width("Pet Item: " + petItem) + 24);
        return width;
    }

    private int displayLevel() {
        if (currentXp >= LEGENDARY_LEVEL_100_XP) return 100 + (int) ((currentXp - LEGENDARY_LEVEL_100_XP) / OVERFLOW_XP_PER_LEVEL);
        return Math.min(100, Math.max(1, petLevel));
    }

    private float levelProgress() {
        if (currentXp > LEGENDARY_LEVEL_100_XP) {
            long overflowXp = currentXp - LEGENDARY_LEVEL_100_XP;
            return (overflowXp % OVERFLOW_XP_PER_LEVEL) * 100.0f / OVERFLOW_XP_PER_LEVEL;
        }
        if (currentXp == LEGENDARY_LEVEL_100_XP) return 100.0f;
        if (petLevel >= 100) return Math.min(100.0f, currentXp * 100.0f / LEGENDARY_LEVEL_100_XP);
        if (tabProgress >= 0.0f) return tabProgress;
        return 0.0f;
    }

    private String formatNumber(long n) { return String.format(Locale.US, "%,d", Math.max(0, n)); }
    private String formatPercent(float p) { return String.format(Locale.US, "%.1f%%", Math.max(0.0f, Math.min(100.0f, p))); }

    private static String findPetName(String text) {
        String cleaned = stripFormatting(text).replaceAll("\\s+", " ").trim().replaceAll("^[✦✧★☆]+\\s*", "").trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String pet : KNOWN_PETS) {
            String target = pet.toLowerCase(Locale.ROOT);
            if (lower.equals(target) || lower.startsWith(target + " ") || lower.endsWith(" " + target) || lower.contains(" " + target + " ")) return pet;
        }
        return null;
    }

    private static String findRarity(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String rarity : new String[]{"Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common"}) if (lower.contains(rarity.toLowerCase(Locale.ROOT))) return rarity;
        return "";
    }

    private static String stripFormatting(String s) { return s == null ? "" : s.replaceAll("§[0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim(); }

    private static long parseNumber(String value) {
        String v = value.replace(",", "").trim().toUpperCase(Locale.ROOT);
        try {
            if (v.endsWith("B")) return (long) (Double.parseDouble(v.substring(0, v.length() - 1)) * 1_000_000_000D);
            if (v.endsWith("M")) return (long) (Double.parseDouble(v.substring(0, v.length() - 1)) * 1_000_000D);
            if (v.endsWith("K")) return (long) (Double.parseDouble(v.substring(0, v.length() - 1)) * 1_000D);
            return (long) Double.parseDouble(v);
        } catch (NumberFormatException e) { return -1L; }
    }

    private static int parseInt(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException e) { return -1; } }
    private static float parsePercent(String value) { try { return Float.parseFloat(value.replace(",", "")); } catch (NumberFormatException e) { return -1.0f; } }

    private void setPet(String name, String rarity, int level) {
        if (name == null || name.isBlank()) return;
        petName = name;
        if (rarity != null && !rarity.isBlank()) petRarity = rarity;
        petLevel = Math.max(1, Math.min(200, level));
        PetCache cached = petCache.get(cacheKey(name));
        if (cached != null && !cached.petItem.isBlank()) petItem = cached.petItem;
        if (cached != null && !cached.icon.isEmpty()) {
            resolvedPetIcon = cached.icon.copy();
            resolvedIconKey = "cache:" + cacheKey(name);
        }
    }

    private static String cacheKey(String name) { return name == null ? "" : name.toLowerCase(Locale.ROOT).trim(); }

    private int rarityColor() {
        return switch (petRarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 0xFFAAAAAA;
            case "uncommon" -> 0xFF55FF55;
            case "rare" -> 0xFF5555FF;
            case "epic" -> 0xFFAA00AA;
            case "legendary" -> 0xFFFFAA00;
            case "mythic" -> 0xFFFF55FF;
            default -> 0xFFFFFFFF;
        };
    }

    private record ParsedPet(String name, String rarity, int level) {}
    private record PetMenuInfo(String type, String rarity, boolean active, String heldItem, double experience, int level) {}
    private static final class PetCache {
        private ItemStack icon = ItemStack.EMPTY;
        private String petItem = "";
        private long menuTotalXp = -1L;
        private String rarity = "";
        private int level = 1;
    }
}
