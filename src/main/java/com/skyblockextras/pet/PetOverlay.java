package com.skyblockextras.pet;

import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetOverlay {
    private final SbeConfig config;

    private String petName = "No Pet";
    private String petRarity = "";
    private int petLevel = 1;
    private long currentXp = 0L;
    private long requiredXp = 0L;
    private long overflowXp = 0L;
    private String petItem = "";
    private int tabScanCooldown = 0;

    private static final Pattern PET_PATTERN = Pattern.compile(
            "(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(.+)");
    private static final Pattern XP_PATTERN = Pattern.compile(
            "(?i)pet\\s*xp\\s*[:：]\\s*([0-9,.]+(?:[kmb])?)");
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "(?i)(?:held item|pet item)\\s*[:：]\\s*(.+)");

    public PetOverlay(SbeConfig config) {
        this.config = config;
    }

    public void tick(Minecraft client) {
        if (!config.petOverlayEnabled || client == null || client.player == null) return;
        if (tabScanCooldown-- > 0) return;
        tabScanCooldown = 5;
        readHypixelTab(client);
    }

    private void readHypixelTab(Minecraft client) {
        StringBuilder all = new StringBuilder();

        try {
            Object tab = client.gui.getTabList();
            for (String fieldName : new String[]{"header", "footer"}) {
                try {
                    Field field = tab.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(tab);
                    if (value instanceof Component component) {
                        all.append(component.getString()).append("\\n");
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        if (client.getConnection() != null) {
            Collection<PlayerInfo> players = client.getConnection().getListedOnlinePlayers();
            for (PlayerInfo info : players) {
                Component display = info.getTabListDisplayName();
                if (display != null) all.append(display.getString()).append("\\n");
            }
        }

        parseTabText(all.toString());
    }

    private void parseTabText(String raw) {
        if (raw == null || raw.isBlank()) return;
        String text = raw.replace('\u00a7', '§');
        String[] lines = text.split("\\R");

        boolean foundPet = false;
        for (String original : lines) {
            String line = stripFormatting(original).trim();
            if (line.isEmpty()) continue;

            Matcher xp = XP_PATTERN.matcher(line);
            if (xp.find()) {
                long total = parseNumber(xp.group(1));
                currentXp = total;
                requiredXp = maxXpForRarity(petRarity);
                overflowXp = Math.max(0L, total - requiredXp);
            }

            Matcher item = ITEM_PATTERN.matcher(line);
            if (item.find()) petItem = item.group(1).trim();

            int petIndex = indexOfIgnoreCase(line, "pet:");
            if (petIndex >= 0) {
                String petText = line.substring(petIndex + 4).trim();
                Matcher pet = PET_PATTERN.matcher(petText);
                if (pet.matches()) {
                    int level = Integer.parseInt(pet.group(1));
                    String details = pet.group(2).trim();
                    String rarity = findRarity(details);
                    String name = removeRarity(details, rarity);
                    if (!name.isBlank()) {
                        setPet(name, rarity, level);
                        foundPet = true;
                    }
                }
            }
        }

        if (foundPet) {
            requiredXp = maxXpForRarity(petRarity);
            overflowXp = Math.max(0L, currentXp - requiredXp);
        }
    }

    private static int indexOfIgnoreCase(String text, String needle) {
        return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
    }

    private static String stripFormatting(String s) {
        return s.replaceAll("§[0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim();
    }

    private static String findRarity(String text) {
        String[] rarities = {"Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common"};
        for (String rarity : rarities) {
            if (text.toLowerCase(Locale.ROOT).contains(rarity.toLowerCase(Locale.ROOT))) return rarity;
        }
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
            return Long.parseLong(v.replace(".", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
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

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!config.petOverlayEnabled || Minecraft.getInstance().player == null) return;

        float scale = config.petScale <= 0 ? 1.0f : config.petScale;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(config.petX, config.petY);
        pose.scale(scale, scale);

        int width = getOverlayWidth();
        int height = getOverlayHeight();
        graphics.fill(-4, -4, width + 4, height + 4, 0xB0000000);

        int textX = 0;
        if (config.showPetIcon) {
            graphics.fill(0, 0, 16, 16, 0xFF444444);
            graphics.outline(0, 0, 16, 16, 0xFFFFFFFF);
            textX = 20;
        }

        StringBuilder name = new StringBuilder();
        if (config.showPetLevel) name.append("[Lvl ").append(petLevel).append("] ");
        name.append(petName);
        if (!petRarity.isBlank()) name.append(" ").append(petRarity);
        graphics.text(Minecraft.getInstance().font, Component.literal(name.toString()), textX, 0, 0xFFFFFFFF, true);

        int y = 12;
        if (config.showPetProgress) {
            int barWidth = 125;
            float progress = requiredXp <= 0 ? 0 : Math.min(1.0f, (float) currentXp / requiredXp);
            graphics.fill(textX, y, textX + barWidth, y + 5, 0xFF333333);
            int fill = Math.round(barWidth * progress);
            if (fill > 0) graphics.fill(textX, y, textX + fill, y + 5, 0xFF55FF55);
            y += 8;
        }
        if (config.showPetXp) {
            graphics.text(Minecraft.getInstance().font, Component.literal("XP: " + formatNumber(currentXp)), textX, y, 0xFFFFFFFF, true);
            y += 10;
        }
        if (config.showOverflowXp) {
            graphics.text(Minecraft.getInstance().font, Component.literal("Overflow: " + formatNumber(overflowXp)), textX, y, 0xFFFFAA00, true);
            y += 10;
        }
        if (config.showPetItem && !petItem.isBlank()) {
            graphics.text(Minecraft.getInstance().font, Component.literal(petItem), textX, y, 0xFFAAAAAA, true);
        }
        pose.popMatrix();
    }

    private int getOverlayWidth() { return config.showPetIcon ? 145 : 125; }
    private int getOverlayHeight() {
        int h = 18;
        if (config.showPetProgress) h += 8;
        if (config.showPetXp) h += 10;
        if (config.showOverflowXp) h += 10;
        if (config.showPetItem && !petItem.isBlank()) h += 10;
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
        currentXp = Math.max(0, current);
        requiredXp = Math.max(0, required);
        overflowXp = Math.max(0, overflow);
    }

    public void setPetItem(String item) { petItem = item == null ? "" : item; }
    public void clearPet() { petName = "No Pet"; petRarity = ""; petLevel = 1; currentXp = 0; requiredXp = 0; overflowXp = 0; petItem = ""; }

    public String getPetName() { return petName; }
    public String getPetRarity() { return petRarity; }
    public int getPetLevel() { return petLevel; }
    public long getCurrentXp() { return currentXp; }
    public long getRequiredXp() { return requiredXp; }
    public long getOverflowXp() { return overflowXp; }
    public String getPetItem() { return petItem; }
}
