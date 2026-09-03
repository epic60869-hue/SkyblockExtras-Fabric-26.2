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

/** Clean SkyBlock pet HUD. Pet data is read from Hypixel's tab list. */
public class PetOverlay {
    private final SbeConfig config;
    private String petName = "No Pet";
    private String petRarity = "";
    private int petLevel = 1;
    private long currentXp = 0L;
    private long requiredXp = 25_353_230L;
    private long overflowXp = 0L;
    private String petItem = "";
    private int tabScanCooldown = 0;

    private static final Pattern PET_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(.+)");
    private static final Pattern XP_PATTERN = Pattern.compile("(?i)pet\\s*xp\\s*[:：]\\s*([0-9,.]+(?:[kmb])?)(?:\\s*/\\s*([0-9,.]+(?:[kmb])?))?");
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)(?:held item|pet item)\\s*[:：]\\s*(.+)");

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
        }
        for (PlayerInfo info : players) all.append(info.getProfile().name()).append('\n');
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
            int petIndex = indexOfIgnoreCase(line, "pet:");
            if (petIndex >= 0) {
                String petText = line.substring(petIndex + 4).trim();
                Matcher pet = PET_PATTERN.matcher(petText);
                if (pet.matches()) {
                    try {
                        int level = Integer.parseInt(pet.group(1));
                        String details = pet.group(2).trim();
                        String rarity = findRarity(details);
                        String name = removeRarity(details, rarity);
                        if (!name.isBlank()) {
                            setPet(name, rarity, level);
                            foundPet = true;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        if (!foundPet) return;
        if (tabXp >= 0L) currentXp = tabXp;
        requiredXp = tabRequired >= 0L ? tabRequired : xpToLevelCap(petLevel, petRarity, petName);
        if (petLevel >= maxPetLevel(petName)) overflowXp = Math.max(0L, currentXp - requiredXp);
        else overflowXp = 0L;
    }

    private static String cleanItemName(String item) { return item.replaceAll("\\s+", " ").trim(); }
    private static int indexOfIgnoreCase(String text, String needle) { return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT)); }
    private static String stripFormatting(String s) { return s.replaceAll("§[0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim(); }

    private static String findRarity(String text) {
        String[] rarities = {"Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common"};
        for (String rarity : rarities) if (text.toLowerCase(Locale.ROOT).contains(rarity.toLowerCase(Locale.ROOT))) return rarity;
        return "";
    }

    private static String removeRarity(String text, String rarity) {
        if (rarity.isBlank()) return text.trim();
        return text.replaceFirst("(?i)\\b" + Pattern.quote(rarity) + "\\b", "").trim();
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

    private static int maxPetLevel(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("golden dragon") || n.contains("jade dragon") || n.contains("rose dragon") ? 200 : 100;
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

    private ItemStack petIcon() {
        String n = petName.toLowerCase(Locale.ROOT);
        if (n.contains("golden dragon") || n.contains("ender dragon")) return new ItemStack(Items.DRAGON_EGG);
        if (n.contains("rabbit")) return new ItemStack(Items.RABBIT);
        if (n.contains("turtle")) return new ItemStack(Items.TURTLE_EGG);
        if (n.contains("bee")) return new ItemStack(Items.HONEYCOMB);
        if (n.contains("wolf") || n.contains("spirit")) return new ItemStack(Items.BONE);
        if (n.contains("sheep")) return new ItemStack(net.minecraft.world.level.block.Blocks.WHITE_WOOL.asItem());
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

    /** Maps every Pet Item currently listed by the Hypixel Wiki to a stable base icon. */
    private ItemStack petItemIcon() {
        String n = petItem.toLowerCase(Locale.ROOT).replaceAll("§.", "").trim();
        if (n.isBlank()) return ItemStack.EMPTY;
        if (n.contains("all skills exp boost") || n.contains("combat exp boost") || n.contains("farming exp boost") || n.contains("fishing exp boost") || n.contains("foraging exp boost") || n.contains("mining exp boost")) return new ItemStack(Items.ENCHANTED_BOOK);
        if (n.contains("antique remedies")) return new ItemStack(Items.GOLD_INGOT);
        if (n.contains("bejeweled collar")) return new ItemStack(Items.DIAMOND);
        if (n.contains("big teeth") || n.contains("bigger teeth") || n.contains("gold claws")) return new ItemStack(Items.GOLD_INGOT);
        if (n.contains("bingo booster")) return new ItemStack(Items.FIREWORK_ROCKET);
        if (n.contains("brown bandana")) return new ItemStack(Items.BROWN_DYE);
        if (n.contains("bubblegum")) return new ItemStack(Items.SLIME_BALL);
        if (n.contains("burnt texts")) return new ItemStack(Items.BOOK);
        if (n.contains("cretan urn")) return new ItemStack(Items.FLOWER_POT);
        if (n.contains("crochet tiger plushie")) return new ItemStack(Items.ORANGE_WOOL);
        if (n.contains("dead cat food")) return new ItemStack(Items.COD);
        if (n.contains("dwarf turtle shelmet") || n.contains("hephaestus shelmet")) return new ItemStack(Items.IRON_HELMET);
        if (n.contains("edible seaweed")) return new ItemStack(Items.DRIED_KELP);
        if (n.contains("eerie toy") || n.contains("eerie treat")) return new ItemStack(Items.PUMPKIN);
        if (n.contains("exp share")) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (n.contains("flying pig")) return new ItemStack(Items.CARROT_ON_A_STICK);
        if (n.contains("four-eyed fish")) return new ItemStack(Items.PUFFERFISH);
        if (n.contains("frog treat")) return new ItemStack(Items.FROGSPAWN);
        if (n.contains("grandma's knitting needle")) return new ItemStack(Items.STRING);
        if (n.contains("green bandana")) return new ItemStack(Items.GREEN_DYE);
        if (n.contains("hardened scales") || n.contains("reinforced scales")) return new ItemStack(Items.IRON_INGOT);
        if (n.contains("hephaestus plushie")) return new ItemStack(net.minecraft.world.level.block.Blocks.WHITE_WOOL.asItem());
        if (n.contains("hephaestus relic") || n.contains("hephaestus remedies")) return new ItemStack(Items.NETHERITE_INGOT);
        if (n.contains("hephaestus souvenir")) return new ItemStack(Items.GOLD_NUGGET);
        if (n.contains("hephaestus urn")) return new ItemStack(Items.FLOWER_POT);
        if (n.contains("iron claws")) return new ItemStack(Items.IRON_INGOT);
        if (n.contains("lucky clover")) return new ItemStack(Items.OAK_LEAVES);
        if (n.contains("minos relic")) return new ItemStack(Items.GOLDEN_APPLE);
        if (n.contains("quick claw")) return new ItemStack(Items.IRON_NUGGET);
        if (n.contains("reaper gem")) return new ItemStack(Items.AMETHYST_SHARD);
        if (n.contains("saddle")) return new ItemStack(Items.SADDLE);
        if (n.contains("serrated claws") || n.contains("sharpened claws")) return new ItemStack(Items.IRON_INGOT);
        if (n.contains("spooky cupcake")) return new ItemStack(Items.CAKE);
        if (n.contains("textbook")) return new ItemStack(Items.BOOK);
        if (n.contains("tier boost")) return new ItemStack(Items.NETHER_STAR);
        if (n.contains("titanium minecart")) return new ItemStack(Items.MINECART);
        if (n.contains("uncommon party hat")) return new ItemStack(Items.PAPER);
        if (n.contains("washed-up souvenir")) return new ItemStack(Items.HEART_OF_THE_SEA);
        if (n.contains("yellow bandana")) return new ItemStack(Items.YELLOW_DYE);
        return new ItemStack(Items.ENCHANTED_BOOK);
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.petOverlayEnabled || Minecraft.getInstance().player == null) return;
        Minecraft client = Minecraft.getInstance();
        float scale = config.petScale <= 0 ? 1.0f : config.petScale;
        int width = getOverlayWidth();
        int height = getOverlayHeight();
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(config.petX, config.petY);
        pose.scale(scale, scale);
        if (config.petBackgroundEnabled) {
            graphics.fill(-6, -6, width + 6, height + 6, 0xD9101117);
            graphics.outline(-6, -6, width + 6, height + 6, 0xFF41434E);
            graphics.fill(-5, -5, -2, height + 5, rarityColor());
        }
        int textX = 0;
        if (config.showPetIcon) { graphics.item(petIcon(), 0, 0); textX = 20; }
        StringBuilder name = new StringBuilder();
        if (config.showPetLevel) name.append("[Lvl ").append(petLevel).append("] ");
        name.append(petName);
        if (!petRarity.isBlank()) name.append(" ").append(petRarity);
        graphics.text(client.font, Component.literal(name.toString()), textX, 1, 0xFFFFFFFF, true);
        int y = 14;
        if (config.showPetProgress) {
            int barWidth = 145;
            float progress = requiredXp <= 0 ? 0 : Math.min(1.0f, currentXp / (float) requiredXp);
            graphics.fill(textX, y, textX + barWidth, y + 5, 0xFF30323A);
            int fill = Math.round(barWidth * progress);
            if (fill > 0) graphics.fill(textX, y, textX + fill, y + 5, rarityColor());
            y += 8;
        }
        if (config.showPetXp) {
            graphics.text(client.font, Component.literal("XP " + formatNumber(currentXp) + " / " + formatNumber(requiredXp)), textX, y, 0xFFDADAE0, false);
            y += 10;
        }
        if (config.showOverflowXp && overflowXp > 0) {
            graphics.text(client.font, Component.literal("Overflow " + formatNumber(overflowXp)), textX, y, 0xFFFFC857, false);
            y += 10;
        }
        if (config.showPetItem && !petItem.isBlank()) {
            ItemStack itemIcon = petItemIcon();
            if (!itemIcon.isEmpty()) graphics.item(itemIcon, textX, y - 2);
            graphics.text(client.font, Component.literal(petItem), textX + 20, y, 0xFFAAAAB3, false);
        }
        pose.popMatrix();
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

    private int getOverlayWidth() { return config.showPetIcon ? 220 : 195; }
    private int getOverlayHeight() {
        int h = 19;
        if (config.showPetProgress) h += 8;
        if (config.showPetXp) h += 10;
        if (config.showOverflowXp && overflowXp > 0) h += 10;
        if (config.showPetItem && !petItem.isBlank()) h += 14;
        return h;
    }
    private String formatNumber(long number) {
        if (number >= 1_000_000_000L) return String.format("%.2fB", number / 1_000_000_000.0);
        if (number >= 1_000_000L) return String.format("%.2fM", number / 1_000_000.0);
        if (number >= 1_000L) return String.format("%.2fK", number / 1_000.0);
        return Long.toString(number);
    }
    public void setPet(String name, String rarity, int level) {
        if (name != null && !name.isBlank()) petName = name;
        if (rarity != null) petRarity = rarity;
        petLevel = Math.max(1, level);
    }
    public void setXp(long current, long required, long overflow) {
        currentXp = Math.max(0, current); requiredXp = Math.max(0, required); overflowXp = Math.max(0, overflow);
    }
    public void setPetItem(String item) { petItem = item == null ? "" : item; }
    public void clearPet() {
        petName = "No Pet"; petRarity = ""; petLevel = 1; currentXp = 0; requiredXp = 25_353_230L; overflowXp = 0; petItem = "";
    }
    public String getPetName() { return petName; }
    public String getPetRarity() { return petRarity; }
    public int getPetLevel() { return petLevel; }
    public long getCurrentXp() { return currentXp; }
    public long getRequiredXp() { return requiredXp; }
    public long getOverflowXp() { return overflowXp; }
    public String getPetItem() { return petItem; }
}
