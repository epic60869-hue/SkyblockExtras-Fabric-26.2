package com.skyblockextras.pet;

import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SkyBlock pet HUD. Pet data is read from Hypixel's tab list. */
public class PetOverlay {
    private final SbeConfig config;
    private String petName = "No Pet";
    private String petRarity = "";
    private int petLevel = 1;
    private long currentXp = 0L;
    private long requiredXp = 25_353_230L;
    private long overflowXp = 0L;
    private int overflowLevel = 1;
    private String petItem = "";
    private int tabScanCooldown = 0;

    private static final Pattern PET_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(.+)");
    private static final Pattern XP_PATTERN = Pattern.compile("(?i)pet\\s*xp\\s*[:：]\\s*([0-9,.]+(?:[kmb])?)(?:\\s*/\\s*([0-9,.]+(?:[kmb])?))?");
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)(?:held item|pet item)\\s*[:：]\\s*(.+)");

    private static final String[] KNOWN_PETS = {
        "Alligator", "Armadillo", "Bat", "Bee", "Black Cat", "Blaze", "Blue Whale", "Chicken",
        "Dolphin", "Ender Dragon", "Enderman", "Elephant", "Ethereal Blaze", "Flying Fish", "Giraffe",
        "Golden Dragon", "Golem", "Griffin", "Guardian", "Hedgehog", "Horse", "Hound", "Jerry",
        "Jellyfish", "Lion", "Magma Cube", "Megalodon", "Monkey", "Mooshroom Cow", "Ocelot", "Parrot",
        "Phoenix", "Pig", "Rabbit", "Rat", "Reindeer", "Rock", "Scatha", "Sheep", "Silverfish",
        "Skeleton", "Skeleton Horse", "Slug", "Snail", "Snowman", "Spirit", "Squid", "Tarantula",
        "Tiger", "Turtle", "Witch", "Wither Skeleton", "Wolf", "Zombie", "Rift Ferret", "Kuudra",
        "Ammonite", "Mithril Golem", "Bal", "Glacite Golem", "Baby Yeti", "T-Rex"
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
        if (tabScanCooldown-- > 0) return;
        tabScanCooldown = 5;
        readHypixelTab(client);
    }

    private void readHypixelTab(Minecraft client) {
        if (client.getConnection() == null) return;
        StringBuilder all = new StringBuilder();
        Collection<PlayerInfo> players = client.getConnection().getListedOnlinePlayers();
        for (PlayerInfo info : players) {
            Component display = info.getTabListDisplayName();
            if (display != null) all.append(display.getString()).append('\n');
            // Some Hypixel tab implementations put the pet text into the profile/display data.
            String profileName = info.getProfile().getName();
            if (profileName != null && !profileName.isBlank()) all.append(profileName).append('\n');
        }
        parseTabText(all.toString());
    }

    private void parseTabText(String raw) {
        if (raw == null || raw.isBlank()) return;
        String[] lines = raw.split("\\R");
        boolean foundPet = false;
        long tabXp = -1L;
        long tabRequired = -1L;

        for (String original : lines) {
            String line = stripFormatting(original);
            if (line.isBlank()) continue;

            Matcher xp = XP_PATTERN.matcher(line);
            if (xp.find()) {
                long parsed = parseNumber(xp.group(1));
                if (parsed >= 0L) tabXp = parsed;
                if (xp.group(2) != null) {
                    long parsedRequired = parseNumber(xp.group(2));
                    if (parsedRequired >= 0L) tabRequired = parsedRequired;
                }
            }

            Matcher item = ITEM_PATTERN.matcher(line);
            if (item.find()) petItem = cleanItemName(item.group(1));

            Matcher pet = PET_PATTERN.matcher(line);
            if (pet.find()) {
                try {
                    int level = Integer.parseInt(pet.group(1));
                    String details = pet.group(2).trim();
                    String rarity = findRarity(details);
                    String detectedName = findKnownPet(details);
                    if (detectedName != null) {
                        setPet(detectedName, rarity, level);
                        foundPet = true;
                    }
                } catch (NumberFormatException ignored) { }
            }

            int petIndex = indexOfIgnoreCase(line, "pet:");
            if (petIndex >= 0) {
                String petText = line.substring(petIndex + 4).trim();
                Matcher explicitPetMatcher = PET_PATTERN.matcher(petText);
                if (explicitPetMatcher.matches()) {
                    try {
                        int level = Integer.parseInt(explicitPetMatcher.group(1));
                        String details = explicitPetMatcher.group(2).trim();
                        String rarity = findRarity(details);
                        String detectedName = findKnownPet(details);
                        if (detectedName != null) {
                            setPet(detectedName, rarity, level);
                            foundPet = true;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
        }

        if (!foundPet) return;
        if (tabXp >= 0L) currentXp = tabXp;
        requiredXp = tabRequired >= 0L ? tabRequired : xpToLevelCap(petLevel, petRarity, petName);
        overflowLevel = calcOverflowLevel(currentXp, petRarity);
        long progressXp = calcLeftOverXp(currentXp, petRarity);
        overflowXp = progressXp >= 0L ? Math.max(0L, progressXp) : Math.max(0L, currentXp - requiredXp);
    }

    private static String findKnownPet(String text) {
        String cleaned = stripFormatting(text).replaceAll("\\s+", " ").trim();
        for (String pet : KNOWN_PETS) {
            if (cleaned.equalsIgnoreCase(pet)) return pet;
            String lower = cleaned.toLowerCase(Locale.ROOT);
            String target = pet.toLowerCase(Locale.ROOT);
            if (lower.startsWith(target + " ") || lower.endsWith(" " + target) || lower.contains(" " + target + " ")) return pet;
        }
        return null;
    }

    private static String cleanItemName(String item) { return item.replaceAll("\\s+", " ").trim(); }
    private static int indexOfIgnoreCase(String text, String needle) { return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT)); }
    private static String stripFormatting(String s) {
        return s.replaceAll("§[0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim();
    }

    private static String findRarity(String text) {
        String[] rarities = {"Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common"};
        for (String rarity : rarities) if (text.toLowerCase(Locale.ROOT).contains(rarity.toLowerCase(Locale.ROOT))) return rarity;
        return "";
    }

    private static long parseNumber(String value) {
        String v = value.replace(",", "").trim().toUpperCase(Locale.ROOT);
        try {
            if (v.endsWith("B")) return Math.round(Double.parseDouble(v.substring(0, v.length() - 1)) * 1_000_000_000D);
            if (v.endsWith("M")) return Math.round(Double.parseDouble(v.substring(0, v.length() - 1)) * 1_000_000D);
            if (v.endsWith("K")) return Math.round(Double.parseDouble(v.substring(0, v.length() - 1)) * 1_000D);
            return Math.round(Double.parseDouble(v));
        } catch (NumberFormatException e) { return -1L; }
    }

    private static long xpToLevelCap(int level, String rarity, String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("golden dragon") || n.contains("jade dragon") || n.contains("rose dragon")) {
            if (level >= 200) return 210_255_385L;
            if (level >= 102) return 25_358_785L + (long) (level - 102) * 1_886_700L;
            if (level >= 101) return 25_353_230L;
        }
        return maxXpForRarity(rarity);
    }

    private static long maxXpForRarity(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 5_624_785L;
            case "uncommon" -> 8_644_220L;
            case "rare" -> 12_626_665L;
            case "epic" -> 18_608_500L;
            case "legendary", "mythic" -> 25_353_230L;
            default -> 25_353_230L;
        };
    }

    private static int getRarityOffset(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 0;
            case "uncommon" -> 6;
            case "rare" -> 11;
            case "epic" -> 15;
            default -> 20;
        };
    }

    private static int getXpForLevel(int level, String rarity) {
        int offset = getRarityOffset(rarity) + Math.max(0, level);
        return offset < PET_XP.length ? PET_XP[offset] : 1_886_700;
    }

    private static int calcOverflowLevel(long xp, String rarity) {
        long exp = Math.max(0L, xp);
        int level = 0;
        while (exp > 0L && level < 1000) { exp -= getXpForLevel(level, rarity); level++; }
        return Math.max(1, level);
    }

    private static long calcLeftOverXp(long xp, String rarity) {
        long exp = Math.max(0L, xp);
        int level = 0;
        while (exp > 0L && level < 1000) {
            long needed = getXpForLevel(level, rarity);
            if (exp > needed) exp -= needed; else return exp;
            level++;
        }
        return -1L;
    }

    private ItemStack petIcon() {
        String n = petName.toLowerCase(Locale.ROOT);
        if (n.contains("dragon")) return new ItemStack(Items.DRAGON_EGG);
        if (n.contains("rabbit")) return new ItemStack(Items.RABBIT);
        if (n.contains("turtle")) return new ItemStack(Items.TURTLE_EGG);
        if (n.contains("bee")) return new ItemStack(Items.HONEYCOMB);
        if (n.contains("wolf") || n.contains("spirit")) return new ItemStack(Items.BONE);
        if (n.contains("sheep")) return new ItemStack(Items.PAPER);
        if (n.contains("pig")) return new ItemStack(Items.PORKCHOP);
        if (n.contains("parrot")) return new ItemStack(Items.COOKIE);
        if (n.contains("bat")) return new ItemStack(Items.PHANTOM_MEMBRANE);
        if (n.contains("silverfish")) return new ItemStack(Items.STONE);
        if (n.contains("slime")) return new ItemStack(Items.SLIME_BALL);
        if (n.contains("magma")) return new ItemStack(Items.MAGMA_CREAM);
        if (n.contains("blaze")) return new ItemStack(Items.BLAZE_ROD);
        if (n.contains("skeleton")) return new ItemStack(Items.SKELETON_SKULL);
        if (n.contains("zombie")) return new ItemStack(Items.ZOMBIE_HEAD);
        if (n.contains("wither")) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (n.contains("enderman")) return new ItemStack(Items.ENDER_PEARL);
        if (n.contains("guardian")) return new ItemStack(Items.PRISMARINE_SHARD);
        if (n.contains("dolphin")) return new ItemStack(Items.COD);
        if (n.contains("squid")) return new ItemStack(Items.INK_SAC);
        return new ItemStack(Items.PLAYER_HEAD);
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.petOverlayEnabled || Minecraft.getInstance().player == null) return;
        Minecraft client = Minecraft.getInstance();
        float scale = config.petScale <= 0 ? 1.0f : config.petScale;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(config.petX, config.petY);
        pose.scale(scale, scale);

        int textX = config.showPetIcon ? 20 : 0;
        int contentHeight = 12;
        if (config.showPetProgress) contentHeight += 10;
        if (config.showPetXp) contentHeight += 10;
        if (config.showOverflowXp && overflowLevel > petLevel) contentHeight += 10;
        if (config.showPetItem && !petItem.isBlank()) contentHeight += 10;

        if (config.petBackgroundEnabled) {
            int contentWidth = getOverlayWidth();
            graphics.fill(textX - 4, -3, contentWidth, contentHeight + 3, 0xB9101117);
            graphics.outline(textX - 4, -3, contentWidth, contentHeight + 3, 0xFF41434E);
            graphics.fill(textX - 4, -3, textX - 1, contentHeight + 3, rarityColor());
        }

        if (config.showPetIcon) graphics.item(petIcon(), 0, 0);
        int y = 0;
        String levelText = config.showPetLevel ? "[Lvl " + petLevel + "] " : "";
        if (!levelText.isEmpty()) graphics.text(client.font, Component.literal(levelText), textX, y, 0xFFFFFFFF, true);
        int petNameX = textX + (levelText.isEmpty() ? 0 : client.font.width(levelText));
        graphics.text(client.font, Component.literal(petName), petNameX, y, rarityColor(), true);
        y += 12;
        if (config.showPetProgress) { graphics.text(client.font, Component.literal("Level Progress: " + formatPercent(levelProgress())), textX, y, 0xFF55FFFF, false); y += 10; }
        if (config.showPetXp) { graphics.text(client.font, Component.literal("Pet XP: " + formatNumber(currentXp)), textX, y, 0xFF55FFFF, false); y += 10; }
        if (config.showOverflowXp && overflowLevel > petLevel) { graphics.text(client.font, Component.literal("Overflow: Lv " + overflowLevel + "  " + formatNumber(overflowXp) + " XP"), textX, y, 0xFFFFC857, false); y += 10; }
        if (config.showPetItem && !petItem.isBlank()) graphics.text(client.font, Component.literal("Pet Item: " + petItem), textX, y, 0xFFAA55FF, false);
        pose.popMatrix();
    }

    private float levelProgress() {
        if (petLevel <= 0) return 0.0f;
        long startXp = getCalculativeXpForLevel(Math.max(0, petLevel - 1), petRarity);
        long needed = getXpForLevel(Math.max(0, petLevel - 1), petRarity);
        if (needed <= 0L) return 0.0f;
        if (currentXp >= startXp + needed) return 100.0f;
        return Math.max(0.0f, Math.min(100.0f, (currentXp - startXp) * 100.0f / needed));
    }

    private static long getCalculativeXpForLevel(int level, String rarity) {
        long xp = 0L;
        for (int i = 0; i < Math.max(0, level); i++) xp += getXpForLevel(i, rarity);
        return xp;
    }

    private int rarityColor() {
        return switch (petRarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 0xFFAAAAAA;
            case "uncommon" -> 0xFF55FF55;
            case "rare" -> 0xFF5555FF;
            case "epic" -> 0xFFAA00AA;
            case "legendary" -> 0xFFFFAA00;
            case "mythic" -> 0xFFFF55FF;
            default -> 0xFFB96BFF;
        };
    }

    private int getOverlayWidth() {
        int width = config.showPetIcon ? 20 : 0;
        width += 155;
        if (petItem.length() > 20) width += Math.min(100, (petItem.length() - 20) * 3);
        return width;
    }

    private String formatPercent(float percent) { return String.format(Locale.ROOT, "%.1f%%", percent); }
    private String formatNumber(long number) { return String.format(Locale.ROOT, "%,d", Math.max(0L, number)); }
    public void setPet(String name, String rarity, int level) { if (name != null && !name.isBlank()) petName = name; if (rarity != null) petRarity = rarity; petLevel = Math.max(1, level); }
    public void setXp(long current, long required, long overflow) { currentXp = Math.max(0, current); requiredXp = Math.max(0, required); overflowXp = Math.max(0, overflow); overflowLevel = calcOverflowLevel(currentXp, petRarity); }
    public void setPetItem(String item) { petItem = item == null ? "" : item; }
    public void clearPet() { petName = "No Pet"; petRarity = ""; petLevel = 1; currentXp = 0L; requiredXp = 25_353_230L; overflowXp = 0L; overflowLevel = 1; petItem = ""; }
    public String getPetName() { return petName; }
    public String getPetRarity() { return petRarity; }
    public int getPetLevel() { return petLevel; }
    public int getOverflowLevel() { return overflowLevel; }
    public long getCurrentXp() { return currentXp; }
    public long getRequiredXp() { return requiredXp; }
    public long getOverflowXp() { return overflowXp; }
    public String getPetItem() { return petItem; }
}
