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

/** Compact SkyBlock pet HUD. Reads the active pet and exact total XP from the Hypixel tab widget. */
public class PetOverlay {
    private final SbeConfig config;
    private String petName = "No Pet";
    private String petRarity = "";
    private int petLevel = 1;
    private int overflowLevel = 0;
    private long currentXp = 0L;
    private long requiredXp = 25_353_230L;
    private float tabProgress = -1.0f;
    private String petItem = "";
    private int tabScanCooldown = 0;

    private static final Pattern PET_PATTERN = Pattern.compile("(?i)\\[?lvl\\s*(\\d+)\\]?\\s+(?:(\\d+)\\s*[♦◆✦])?\\s*(.+)");
    private static final Pattern PET_XP_LINE = Pattern.compile("(?i)([0-9,.]+(?:[kmb])?)\\s*/\\s*([0-9,.]+(?:[kmb])?)\\s*XP(?:\\s*\\(([0-9,.]+)%\\))?");
    private static final Pattern PET_LABEL_XP = Pattern.compile("(?i)pet\\s*xp\\s*[:：]\\s*([0-9,.]+(?:[kmb])?)(?:\\s*/\\s*([0-9,.]+(?:[kmb])?))?");
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)(?:held item|pet item)\\s*[:：]\\s*(.+)");

    private static final String[] KNOWN_PETS = {
        "Alligator", "Armadillo", "Bal", "Bat", "Bee", "Black Cat", "Blaze", "Blue Whale", "Chicken",
        "Dolphin", "Ender Dragon", "Enderman", "Elephant", "Ethereal Blaze", "Flying Fish", "Giraffe",
        "Golden Dragon", "Golem", "Griffin", "Guardian", "Hedgehog", "Horse", "Hound", "Jerry", "Jellyfish",
        "Lion", "Magma Cube", "Megalodon", "Mithril Golem", "Monkey", "Mooshroom Cow", "Ocelot", "Parrot",
        "Phoenix", "Pig", "Rabbit", "Rat", "Reindeer", "Rock", "Rose Dragon", "Scatha", "Sheep", "Silverfish",
        "Skeleton", "Skeleton Horse", "Slug", "Snail", "Snowman", "Spirit", "Squid", "Tarantula", "Tiger",
        "Turtle", "Witch", "Wither Skeleton", "Wolf", "Zombie", "Rift Ferret", "Kuudra", "Ammonite",
        "Glacite Golem", "Baby Yeti", "T-Rex", "T-Rex Pet"
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
        }
        parseTabText(all.toString());
    }

    private void parseTabText(String raw) {
        if (raw == null || raw.isBlank()) return;
        boolean foundPet = false;
        long levelLocalXp = -1L;
        long levelRequired = -1L;
        float parsedProgress = -1.0f;

        for (String original : raw.split("\\R")) {
            String line = stripFormatting(original);
            if (line.isBlank()) continue;

            Matcher xp = PET_LABEL_XP.matcher(line);
            if (xp.find()) {
                levelLocalXp = parseNumber(xp.group(1));
                if (xp.group(2) != null) levelRequired = parseNumber(xp.group(2));
            }

            Matcher xpLine = PET_XP_LINE.matcher(line);
            if (xpLine.find()) {
                levelLocalXp = parseNumber(xpLine.group(1));
                levelRequired = parseNumber(xpLine.group(2));
                if (xpLine.group(3) != null) parsedProgress = parsePercent(xpLine.group(3));
            }

            Matcher item = ITEM_PATTERN.matcher(line);
            if (item.find()) petItem = cleanItemName(item.group(1));

            Matcher pet = PET_PATTERN.matcher(line);
            if (pet.find()) {
                try {
                    int level = Integer.parseInt(pet.group(1));
                    int parsedOverflow = pet.group(2) == null ? 0 : Integer.parseInt(pet.group(2));
                    String details = pet.group(3).trim();
                    String detectedName = findPetName(details);
                    boolean explicitOverflowMarker = pet.group(2) != null;
                    if (detectedName != null || explicitOverflowMarker) {
                        if (detectedName == null) detectedName = cleanPetDetails(details);
                        setPet(detectedName, findRarity(details), level);
                        overflowLevel = Math.max(0, parsedOverflow);
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
                        int parsedOverflow = explicitPetMatcher.group(2) == null ? 0 : Integer.parseInt(explicitPetMatcher.group(2));
                        String details = explicitPetMatcher.group(3).trim();
                        String detectedName = findPetName(details);
                        if (detectedName != null) {
                            setPet(detectedName, findRarity(details), level);
                            overflowLevel = Math.max(0, parsedOverflow);
                            foundPet = true;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
        }

        if (!foundPet) return;

        /*
         * Hypixel's tab widget reports the XP INSIDE the current level:
         *   1,175,300.9/1.9M XP (62.3%)
         * The HUD, however, wants the pet's total accumulated XP.
         *
         * For normal pets we add all completed levels plus current-level XP.
         * The old implementation used the generic curve for every pet, which
         * is wrong for extended/Dragon pets and produced the ~2m discrepancy.
         */
        if (levelLocalXp >= 0L) {
            if (petLevel >= 200) {
                // At level 200, the tab value is already all the XP relevant to
                // the current overflow level calculation; don't add a second curve.
                currentXp = calculateTotalForDisplayedLevel(petLevel, levelLocalXp, petRarity);
            } else {
                currentXp = calculateTotalForDisplayedLevel(petLevel, levelLocalXp, petRarity);
            }
        }

        if (levelRequired >= 0L) requiredXp = levelRequired;
        if (parsedProgress >= 0.0f) tabProgress = parsedProgress;
    }

    private long calculateTotalForDisplayedLevel(int level, long localXp, String rarity) {
        if (level <= 1) return Math.max(0L, localXp);

        // SkyBlock's displayed pet XP curve is cumulative. The special 101-200
        // curve is represented in PET_XP above. Sum completed levels, then add
        // the exact decimal XP currently inside the displayed level.
        long completed = 0L;
        int completedLevels = Math.min(level - 1, 200);
        for (int i = 0; i < completedLevels; i++) completed += getXpForLevel(i, rarity);
        return completed + Math.max(0L, localXp);
    }

    private static String findPetName(String text) {
        String cleaned = cleanPetDetails(text);
        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String pet : KNOWN_PETS) {
            String target = pet.toLowerCase(Locale.ROOT);
            if (lower.equals(target) || lower.startsWith(target + " ") || lower.endsWith(" " + target) || lower.contains(" " + target + " ")) return pet;
        }
        return null;
    }

    private static String cleanPetDetails(String text) {
        return stripFormatting(text).replaceAll("\\s+", " ").trim().replaceAll("^[✦✧★☆]+\\s*", "").trim();
    }

    private static String cleanItemName(String item) { return item.replaceAll("\\s+", " ").trim(); }
    private static int indexOfIgnoreCase(String text, String needle) { return text.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT)); }
    private static String stripFormatting(String s) { return s.replaceAll("§[0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim(); }
    private static String findRarity(String text) { for (String rarity : new String[]{"Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common"}) if (text.toLowerCase(Locale.ROOT).contains(rarity.toLowerCase(Locale.ROOT))) return rarity; return ""; }
    private static long parseNumber(String value) { String v=value.replace(",","").trim().toUpperCase(Locale.ROOT); try { if(v.endsWith("B")) return Math.round(Double.parseDouble(v.substring(0,v.length()-1))*1_000_000_000D); if(v.endsWith("M")) return Math.round(Double.parseDouble(v.substring(0,v.length()-1))*1_000_000D); if(v.endsWith("K")) return Math.round(Double.parseDouble(v.substring(0,v.length()-1))*1_000D); return Math.round(Double.parseDouble(v)); } catch(NumberFormatException e){return -1L;} }
    private static float parsePercent(String value) { try { return Float.parseFloat(value.replace(",", "")); } catch (NumberFormatException e) { return -1.0f; } }
    private static int getRarityOffset(String rarity){return switch(rarity.toLowerCase(Locale.ROOT)){case "common"->0;case "uncommon"->6;case "rare"->11;case "epic"->15;default->20;};}
    private static int getXpForLevel(int level,String rarity){int offset=getRarityOffset(rarity)+Math.max(0,level);return offset<PET_XP.length?PET_XP[offset]:1_886_700;}
    private ItemStack petIcon(){String n=petName.toLowerCase(Locale.ROOT);if(n.contains("dragon"))return new ItemStack(Items.DRAGON_EGG);if(n.contains("rabbit"))return new ItemStack(Items.RABBIT);if(n.contains("turtle"))return new ItemStack(Items.TURTLE_EGG);if(n.contains("bee"))return new ItemStack(Items.HONEYCOMB);if(n.contains("wolf")||n.contains("spirit"))return new ItemStack(Items.BONE);if(n.contains("sheep"))return new ItemStack(Items.PAPER);if(n.contains("pig"))return new ItemStack(Items.PORKCHOP);if(n.contains("parrot"))return new ItemStack(Items.COOKIE);if(n.contains("bat"))return new ItemStack(Items.PHANTOM_MEMBRANE);if(n.contains("silverfish"))return new ItemStack(Items.STONE);if(n.contains("slime"))return new ItemStack(Items.SLIME_BALL);if(n.contains("magma"))return new ItemStack(Items.MAGMA_CREAM);if(n.contains("blaze"))return new ItemStack(Items.BLAZE_ROD);if(n.contains("skeleton"))return new ItemStack(Items.SKELETON_SKULL);if(n.contains("zombie"))return new ItemStack(Items.ZOMBIE_HEAD);if(n.contains("wither"))return new ItemStack(Items.WITHER_SKELETON_SKULL);if(n.contains("enderman"))return new ItemStack(Items.ENDER_PEARL);if(n.contains("guardian"))return new ItemStack(Items.PRISMARINE_SHARD);if(n.contains("dolphin"))return new ItemStack(Items.COD);if(n.contains("squid"))return new ItemStack(Items.INK_SAC);return new ItemStack(Items.PLAYER_HEAD);}
    public void render(GuiGraphicsExtractor graphics,DeltaTracker deltaTracker){if(!config.petOverlayEnabled||Minecraft.getInstance().player==null)return;Minecraft client=Minecraft.getInstance();float scale=config.petScale<=0?1.0f:config.petScale;var pose=graphics.pose();pose.pushMatrix();pose.translate(config.petX,config.petY);pose.scale(scale,scale);int textX=config.showPetIcon?20:0;int contentHeight=12;if(config.showPetProgress)contentHeight+=10;if(config.showPetXp)contentHeight+=10;if(config.showPetItem&&!petItem.isBlank())contentHeight+=10;if(config.petBackgroundEnabled){int contentWidth=getOverlayWidth();graphics.fill(textX-4,-3,contentWidth,contentHeight+3,0xB9101117);graphics.outline(textX-4,-3,contentWidth,contentHeight+3,0xFF41434E);graphics.fill(textX-4,-3,textX-1,contentHeight+3,rarityColor());}if(config.showPetIcon)graphics.item(petIcon(),0,0);int y=0;String levelText=config.showPetLevel?"[Lvl "+displayLevel()+"] ":"";if(!levelText.isEmpty())graphics.text(client.font,Component.literal(levelText),textX,y,0xFFFFFFFF,true);int petNameX=textX+(levelText.isEmpty()?0:client.font.width(levelText));graphics.text(client.font,Component.literal(petName),petNameX,y,rarityColor(),true);y+=12;if(config.showPetProgress){graphics.text(client.font,Component.literal("Level Progress: "+formatPercent(levelProgress())),textX,y,0xFF55FFFF,false);y+=10;}if(config.showPetXp){graphics.text(client.font,Component.literal("Pet XP: "+formatNumber(currentXp)),textX,y,0xFF55FFFF,false);y+=10;}if(config.showPetItem&&!petItem.isBlank())graphics.text(client.font,Component.literal("Pet Item: "+petItem),textX,y,0xFFAA55FF,false);pose.popMatrix();}
    private int displayLevel(){return petLevel>=200?200+overflowLevel:petLevel;}
    private float levelProgress(){if(tabProgress>=0.0f)return tabProgress;if(requiredXp<=0)return 0.0f;long levelStart=getCalculativeXpForLevel(Math.max(0,petLevel-1),petRarity);return Math.max(0,Math.min(100,(currentXp-levelStart)*100.0f/requiredXp));}
    private long getCalculativeXpForLevel(int level,String rarity){long xp=0;for(int i=0;i<Math.max(0,level);i++)xp+=getXpForLevel(i,rarity);return xp;}
    private String formatNumber(long n){return String.format(Locale.US,"%,d",Math.max(0,n));}
    private String formatPercent(float p){return String.format(Locale.US,"%.1f%%",p);}
    private void setPet(String name,String rarity,int level){petName=name;petRarity=rarity==null?"":rarity;petLevel=Math.max(1,level);}
    private int rarityColor(){return switch(petRarity.toLowerCase(Locale.ROOT)){case "common"->0xFFAAAAAA;case "uncommon"->0xFF55FF55;case "rare"->0xFF5555FF;case "epic"->0xFFAA00AA;case "legendary"->0xFFFFAA00;case "mythic"->0xFFFF55FF;default->0xFFFFFFFF;};}
    private int getOverlayWidth(){Minecraft client=Minecraft.getInstance();String levelText=config.showPetLevel?"[Lvl "+displayLevel()+"] ":"";String first=levelText+petName;int width=config.showPetIcon?20:0;int w=client.font.width(first);if(config.showPetProgress)w=Math.max(w,client.font.width("Level Progress: 100.0%"));if(config.showPetXp)w=Math.max(w,client.font.width("Pet XP: 999,999,999"));if(config.showPetItem&&!petItem.isBlank())w=Math.max(w,client.font.width("Pet Item: "+petItem));return width+w+4;}
}