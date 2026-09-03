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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pet HUD. TAB is the live source; the Pets menu only enriches/caches the current pet. */
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
    private final Map<String, PetCache> petCache = new HashMap<>();

    private static final Pattern PET_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(?:(\\d+)\\s*[♦◆✦])?\\s*(.+)");
    private static final Pattern PET_XP_LINE = Pattern.compile("(?i)([0-9,.]+(?:[kmb])?)\\s*/\\s*([0-9,.]+(?:[kmb])?)\\s*XP(?:\\s*\\(([0-9,.]+)%\\))?");
    private static final Pattern PET_LABEL_XP = Pattern.compile("(?i)pet\\s*xp\\s*[:：]\\s*([0-9,.]+(?:[kmb])?)");
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)(?:held item|pet item)\\s*[:：]\\s*(.+)");
    private static final Pattern ENTITY_LEVEL_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?");
    private static final Pattern HELD_ITEM_LORE = Pattern.compile("(?i)^held item\\s*:\\s*(.+)$");

    private static final String ROSE_DRAGON_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTc2MTE3MzAyMjM0NywKICAicHJvZmlsZUlkIiA6ICJjOWI3OWY2OGEyZGY0YjA1ODUwOWRlMzg5YjM5ZDUyYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ3cmVlcGVyX2Jvb3AiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI3YzNkZTA3NWEyYmIyMzhlZjUxNDMxMjA2YjEwZDU4NmNiMmE1YjFjYzQxZmU4NTFjYzVmMGIwMmQzNTdjNyIsCiAgICAgICAgIm1ldGFkYXRhIiA6IHsibW9kZWwiIDogInNsaW0ifQogICAgfQogIH0KfQ==";

    private static final String[] KNOWN_PETS = {
            "Alligator","Ammonite","Ankylosaurus","Armadillo","Baby Yeti","Bal","Bat","Bee","Black Cat","Blaze",
            "Blue Whale","Chicken","Dolphin","Elephant","Ender Dragon","Enderman","Endermite","Flying Fish","Giraffe",
            "Golden Dragon","Golem","Glacite Golem","Grandma Wolf","Griffin","Guardian","Hedgehog","Horse","Hound",
            "Jade Dragon","Jellyfish","Jerry","Kuudra","Lion","Magma Cube","Megalodon","Mithril Golem","Mole","Monkey",
            "Mooshroom Cow","Mosquito","Ocelot","Parrot","Phoenix","Pig","Pigman","Rabbit","Rat","Reindeer","Rock",
            "Rose Dragon","Scatha","Sheep","Silverfish","Skeleton","Skeleton Horse","Slug","Snail","Snowman","Spirit",
            "Squid","Tarantula","Tiger","Turtle","Wisp","Wither Skeleton","Wolf","Zombie","Rift Ferret"
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
        // Always read TAB first. The menu is deliberately secondary and can never
        // clear the state when it closes.
        readHypixelTab(client);
        readOpenedPetsMenu(client);
        resolveRealPetIcon(client);
    }

    private void readHypixelTab(Minecraft client) {
        if (client.getConnection() == null) return;
        UUID localUuid = client.player.getUUID();
        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            if (info.getProfile() == null || !localUuid.equals(info.getProfile().id())) continue;
            Component display = info.getTabListDisplayName();
            if (display == null) return;
            String text = display.getString();
            if (!containsPetWidget(text)) return;
            ParsedPet parsed = parseTabPet(text);
            if (parsed != null) applyTabPet(parsed, text);
            return;
        }
    }

    private static boolean containsPetWidget(String text) {
        if (text == null) return false;
        String lower = stripFormatting(text).toLowerCase(Locale.ROOT);
        return lower.contains("pet:");
    }

    private ParsedPet parseTabPet(String raw) {
        String[] lines = raw.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = stripFormatting(lines[i]);
            if (line.equalsIgnoreCase("Pet:") || line.toLowerCase(Locale.ROOT).startsWith("pet:")) {
                String inline = line.length() > 4 ? line.substring(4).trim() : "";
                if (!inline.isBlank()) {
                    ParsedPet p = parsePetLine(inline);
                    if (p != null) return p.withStart(i + 1, lines);
                }
                for (int j = i + 1; j < Math.min(lines.length, i + 8); j++) {
                    ParsedPet p = parsePetLine(stripFormatting(lines[j]));
                    if (p != null) return p.withStart(j + 1, lines);
                }
            }
        }
        return null;
    }

    private ParsedPet parsePetLine(String line) {
        Matcher m = PET_PATTERN.matcher(line);
        if (!m.find()) return null;
        try {
            int level = Integer.parseInt(m.group(1));
            String details = m.group(3).trim();
            String name = findPetName(details);
            if (name == null) return null;
            return new ParsedPet(name, findRarity(details), level, null, null);
        } catch (NumberFormatException ignored) { return null; }
    }

    private void applyTabPet(ParsedPet parsed, String raw) {
        boolean changed = !parsed.name.equalsIgnoreCase(petName);
        PetCache cached = petCache.get(cacheKey(parsed.name));

        if (changed) {
            // Switch to the new pet immediately, but restore only data cached for
            // that exact pet. The previous pet's XP/item/icon is never reused.
            petName = parsed.name;
            petRarity = parsed.rarity;
            petLevel = clampLevel(parsed.level);
            currentXp = cached != null && cached.totalXp >= 0 ? cached.totalXp : 0L;
            petItem = cached != null ? cached.petItem : "";
            resolvedPetIcon = cached != null ? cached.icon.copy() : ItemStack.EMPTY;
            tabProgress = -1.0f;
        } else {
            setPet(parsed.name, parsed.rarity, parsed.level);
        }

        String[] lines = raw.split("\\R");
        int petLine = findPetLine(lines, parsed.name);
        long localXp = -1L;
        float progress = -1.0f;
        String item = "";
        for (int i = petLine + 1; i < Math.min(lines.length, petLine + 9); i++) {
            String line = stripFormatting(lines[i]);
            Matcher xp = PET_XP_LINE.matcher(line);
            if (xp.find()) {
                localXp = parseNumber(xp.group(1));
                if (xp.group(3) != null) progress = parsePercent(xp.group(3));
                continue;
            }
            Matcher label = PET_LABEL_XP.matcher(line);
            if (label.find()) { localXp = parseNumber(label.group(1)); continue; }
            Matcher held = ITEM_PATTERN.matcher(line);
            if (held.find()) item = held.group(1).trim();
        }

        if (localXp >= 0) currentXp = calculateTotalXp(petLevel, localXp, petRarity);
        if (progress >= 0) tabProgress = progress;
        if (!item.isBlank()) petItem = item;

        PetCache state = petCache.computeIfAbsent(cacheKey(petName), k -> new PetCache());
        state.totalXp = currentXp;
        state.petItem = petItem;
        state.level = petLevel;
        state.rarity = petRarity;
        if (!resolvedPetIcon.isEmpty()) state.icon = resolvedPetIcon.copy();
    }

    private int findPetLine(String[] lines, String name) {
        for (int i = 0; i < lines.length; i++) if (stripFormatting(lines[i]).toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) return i;
        return 0;
    }

    private void readOpenedPetsMenu(Minecraft client) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        String title = stripFormatting(screen.getTitle().getString());
        if (!title.matches("(?:\\(\\d+/\\d+\\) )?Pets(?:.*)?")) return;

        for (var slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            PetMenuInfo info = readPetMenuInfo(stack);
            if (info == null || !(info.active || isActivePetByLore(stack))) continue;
            String name = findPetName(info.type);
            if (name == null) name = info.type;
            String key = cacheKey(name);
            PetCache cache = petCache.computeIfAbsent(key, k -> new PetCache());
            cache.rarity = info.rarity;
            cache.level = info.level;
            if (info.experience >= 0) cache.totalXp = (long) info.experience;
            String held = heldItemFromLore(stack);
            if (held.isBlank()) held = info.heldItem;
            if (held != null && !held.isBlank()) cache.petItem = stripFormatting(held).trim();
            if (stack.get(DataComponents.PROFILE) != null) cache.icon = stack.copy();

            // Only apply menu data to the currently equipped pet. This is the
            // important part: closing the menu will not erase it.
            if (name.equalsIgnoreCase(petName)) {
                petRarity = cache.rarity.isBlank() ? petRarity : cache.rarity;
                petLevel = cache.level > 0 ? cache.level : petLevel;
                if (cache.totalXp >= 0) currentXp = cache.totalXp;
                if (!cache.petItem.isBlank()) petItem = cache.petItem;
                if (!cache.icon.isEmpty()) resolvedPetIcon = cache.icon.copy();
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
            var attrs = root.getCompoundOrEmpty("ExtraAttributes");
            String raw = attrs.getStringOr("petInfo", root.getStringOr("petInfo", ""));
            if (raw.isBlank()) return null;
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (!json.has("type")) return null;
            String type = json.get("type").getAsString();
            String rarity = json.has("tier") ? json.get("tier").getAsString() : "";
            boolean active = json.has("active") && json.get("active").getAsBoolean();
            String held = json.has("heldItem") ? json.get("heldItem").getAsString() : "";
            double exp = json.has("exp") ? json.get("exp").getAsDouble() : -1;
            int level = exp >= 0 ? calculateLevelFromTotalXp((long) exp, rarity) : 1;
            return new PetMenuInfo(type, rarity, active, held, exp, level);
        } catch (Exception ignored) { return null; }
    }

    private boolean isActivePetByLore(ItemStack stack) {
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        return lore.lines().stream().anyMatch(line -> stripFormatting(line.getString()).contains("Click to despawn!"));
    }

    private String heldItemFromLore(ItemStack stack) {
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        for (Component line : lore.lines()) {
            Matcher m = HELD_ITEM_LORE.matcher(stripFormatting(line.getString()).trim());
            if (m.matches()) return m.group(1).trim();
        }
        return "";
    }

    private void resolveRealPetIcon(Minecraft client) {
        if (petName.equalsIgnoreCase("No Pet")) return;
        PetCache cached = petCache.get(cacheKey(petName));
        if (cached != null && !cached.icon.isEmpty()) { resolvedPetIcon = cached.icon.copy(); return; }
        if (petName.equalsIgnoreCase("Rose Dragon")) {
            ItemStack rose = createTexturedHead(ROSE_DRAGON_TEXTURE, "Rose Dragon");
            if (!rose.isEmpty()) { resolvedPetIcon = rose; petCache.computeIfAbsent(cacheKey(petName), k -> new PetCache()).icon = rose.copy(); return; }
        }
        if (client.level == null || client.player == null) return;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !living.isAlive()) continue;
            if (living.distanceToSqr(client.player) > 18 * 18) continue;
            Component custom = living.getCustomName();
            if (custom == null || !stripFormatting(custom.getString()).toLowerCase(Locale.ROOT).contains(petName.toLowerCase(Locale.ROOT))) continue;
            ItemStack head = findHeadNear(client, living, 3.0D);
            if (!head.isEmpty()) { resolvedPetIcon = head; petCache.computeIfAbsent(cacheKey(petName), k -> new PetCache()).icon = head.copy(); return; }
        }
    }

    private ItemStack findHeadNear(Minecraft client, LivingEntity anchor, double radius) {
        var box = anchor.getBoundingBox().inflate(radius);
        for (Entity entity : client.level.getEntities((Entity) null, box, Entity::isAlive)) {
            ItemStack head = ItemStack.EMPTY;
            if (entity instanceof ArmorStand stand) head = stand.getItemBySlot(EquipmentSlot.HEAD);
            else if (entity instanceof Display.ItemDisplay display) head = display.getItemStack();
            if (!head.isEmpty() && head.getItem() == Items.PLAYER_HEAD && head.get(DataComponents.PROFILE) != null) return head.copy();
        }
        return ItemStack.EMPTY;
    }

    private ItemStack createTexturedHead(String textureValue, String name) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8));
            Property property = new Property("textures", textureValue);
            PropertyMap properties = new PropertyMap(ImmutableMultimap.<String, Property>builder().put("textures", property).build());
            GameProfile profile = new GameProfile(uuid, "SkyBlockPet", properties);
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            return stack;
        } catch (Throwable ignored) { return ItemStack.EMPTY; }
    }

    private long calculateTotalXp(int level, long localXp, String rarity) {
        if (level > 100) return LEGENDARY_LEVEL_100_XP + (long)(level - 100) * OVERFLOW_XP_PER_LEVEL + Math.max(0, localXp);
        long total = 0;
        int offset = getRarityOffset(rarity);
        for (int i = 0; i < Math.min(level - 1, 99); i++) if (offset + i < PET_XP.length) total += PET_XP[offset + i];
        return total + Math.max(0, localXp);
    }

    private static int calculateLevelFromTotalXp(long totalXp, String rarity) {
        if (totalXp >= LEGENDARY_LEVEL_100_XP) return 100 + (int)((totalXp - LEGENDARY_LEVEL_100_XP) / OVERFLOW_XP_PER_LEVEL);
        long spent = 0;
        int offset = getRarityOffset(rarity);
        for (int level = 1; level < 100; level++) {
            int index = offset + level - 1;
            long req = index < PET_XP.length ? PET_XP[index] : 1;
            if (totalXp < spent + req) return level;
            spent += req;
        }
        return 100;
    }

    private static int getRarityOffset(String rarity) {
        return switch (rarity == null ? "" : rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 0; case "uncommon" -> 6; case "rare" -> 11; case "epic" -> 16; default -> 20;
        };
    }

    private int displayLevel() {
        if (currentXp >= LEGENDARY_LEVEL_100_XP) return 100 + (int)((currentXp - LEGENDARY_LEVEL_100_XP) / OVERFLOW_XP_PER_LEVEL);
        return Math.min(100, Math.max(1, petLevel));
    }

    private String progressLine() {
        if (currentXp >= LEGENDARY_LEVEL_100_XP) {
            long progress = (currentXp - LEGENDARY_LEVEL_100_XP) % OVERFLOW_XP_PER_LEVEL;
            return formatNumber(progress) + " / " + formatNumber(OVERFLOW_XP_PER_LEVEL);
        }
        if (petLevel >= 100) return formatNumber(Math.min(currentXp, LEGENDARY_LEVEL_100_XP)) + " / " + formatNumber(LEGENDARY_LEVEL_100_XP);
        int offset = getRarityOffset(petRarity), index = offset + petLevel - 1;
        long requirement = index >= 0 && index < PET_XP.length ? PET_XP[index] : 1;
        long completed = 0;
        for (int i = 0; i < petLevel - 1; i++) if (offset+i < PET_XP.length) completed += PET_XP[offset+i];
        return formatNumber(Math.min(requirement, Math.max(0, currentXp - completed))) + " / " + formatNumber(requirement);
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!config.petOverlayEnabled || client.player == null || petName.equals("No Pet")) return;
        float scale = config.petScale <= 0 ? 1 : config.petScale;
        var pose = graphics.pose(); pose.pushMatrix(); pose.translate(config.petX, config.petY); pose.scale(scale, scale);
        int textX = config.showPetIcon ? 20 : 0;
        boolean showProgress = config.showPetProgress;
        int height = 12 + (showProgress ? 10 : 0) + (config.showPetXp ? 10 : 0) + (config.showPetItem && !petItem.isBlank() ? 10 : 0);
        int width = Math.max(client.font.width("[Lvl 999] " + petName), client.font.width("25,353,230 / 25,353,230")) + 24;
        if (config.showPetItem) width = Math.max(width, client.font.width("Pet Item: " + petItem) + 24);
        if (config.petBackgroundEnabled) { graphics.fill(textX-4,-3,width,height+3,0xB9101117); graphics.outline(textX-4,-3,width,height+3,0xFF41434E); }
        if (config.showPetIcon && !resolvedPetIcon.isEmpty()) graphics.item(resolvedPetIcon,0,0);
        int y=0; String levelText=config.showPetLevel ? "[Lvl "+displayLevel()+"] " : "";
        if (!levelText.isEmpty()) graphics.text(client.font,Component.literal(levelText),textX,y,0xFFAAAAAA,true);
        graphics.text(client.font,Component.literal(petName),textX+client.font.width(levelText),y,rarityColor(),true); y+=12;
        if (showProgress) { graphics.text(client.font,Component.literal(progressLine()),textX,y,0xFF55FFFF,true); y+=10; }
        if (config.showPetXp) { graphics.text(client.font,Component.literal(formatNumber(currentXp)+" XP"),textX,y,0xFF55FFFF,true); y+=10; }
        if (config.showPetItem && !petItem.isBlank()) graphics.text(client.font,Component.literal("Pet Item: "+petItem),textX,y,0xFFAA55FF,true);
        pose.popMatrix();
    }

    private void setPet(String name,String rarity,int level){ if(name==null||name.isBlank())return; petName=name; if(rarity!=null&&!rarity.isBlank())petRarity=rarity; petLevel=clampLevel(level); }
    private static int clampLevel(int level){return Math.min(200,Math.max(1,level));}
    private static String findPetName(String text){String lower=stripFormatting(text).toLowerCase(Locale.ROOT);for(String pet:KNOWN_PETS){String t=pet.toLowerCase(Locale.ROOT);if(lower.equals(t)||lower.startsWith(t+" ")||lower.contains(" "+t+" ")||lower.endsWith(" "+t))return pet;}return null;}
    private static String findRarity(String text){String lower=text.toLowerCase(Locale.ROOT);for(String r:new String[]{"Mythic","Legendary","Epic","Rare","Uncommon","Common"})if(lower.contains(r.toLowerCase(Locale.ROOT)))return r;return "";}
    private static String stripFormatting(String s){return s==null?"":s.replaceAll("§[0-9a-fk-orx]","").replaceAll("\\s+"," ").trim();}
    private static long parseNumber(String value){try{String v=value.replace(",","").trim().toUpperCase(Locale.ROOT);if(v.endsWith("B"))return(long)(Double.parseDouble(v.substring(0,v.length()-1))*1_000_000_000D);if(v.endsWith("M"))return(long)(Double.parseDouble(v.substring(0,v.length()-1))*1_000_000D);if(v.endsWith("K"))return(long)(Double.parseDouble(v.substring(0,v.length()-1))*1_000D);return(long)Double.parseDouble(v);}catch(Exception e){return-1;}}
    private static float parsePercent(String value){try{return Float.parseFloat(value.replace(",",""));}catch(Exception e){return-1;}}
    private String formatNumber(long n){return String.format(Locale.US,"%,d",Math.max(0,n));}
    private int rarityColor(){return switch(petRarity.toLowerCase(Locale.ROOT)){case "common"->0xFFAAAAAA;case "uncommon"->0xFF55FF55;case "rare"->0xFF5555FF;case "epic"->0xFFAA00AA;case "legendary"->0xFFFFAA00;case "mythic"->0xFFFF55FF;default->0xFFFFFFFF;};}
    private static String cacheKey(String name){return name==null?"":name.toLowerCase(Locale.ROOT).trim();}

    private record ParsedPet(String name,String rarity,int level,Integer unused,Object unused2){ ParsedPet withStart(int start,String[] lines){return this;} }
    private record PetMenuInfo(String type,String rarity,boolean active,String heldItem,double experience,int level){}
    private static final class PetCache { private ItemStack icon=ItemStack.EMPTY; private String petItem=""; private long totalXp=-1; private String rarity=""; private int level=1; }
}