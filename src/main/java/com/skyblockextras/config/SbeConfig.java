package com.skyblockextras.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public class SbeConfig {
    public boolean farmingRngEnabled = true;
    public boolean harvestFeastEnabled = true;
    public boolean slugEnabled = true;
    public boolean dyesEnabled = true;

    public boolean petOverlayEnabled = true;
    public boolean petBackgroundEnabled = true;
    public boolean showPetIcon = true;
    public boolean showPetLevel = true;
    public boolean showPetProgress = true;
    public boolean showPetXp = true;
    public boolean showOverflowXp = true;
    public boolean showPetItem = true;
    public float petScale = 1.0f;
    public int petX = 10;
    public int petY = 10;

    // RNG drop announcement overlay settings.
    public boolean rngDropOverlayEnabled = true;
    public boolean rngDropOverlayBackgroundEnabled = true;
    public float rngDropOverlayScale = 1.0f;
    public int rngDropOverlayX = -1;
    public int rngDropOverlayY = -1;
    // SHORT = 1.25M, FULL = 1,250,000, COINS = 1.25M coins.
    public String rngDropPriceFormat = "SHORT";

    public Map<String, Boolean> harvestFeastDrops = new LinkedHashMap<>();
    public Map<String, Boolean> farmingDyes = new LinkedHashMap<>();
    public boolean epicSlug = true;
    public boolean legendarySlug = true;
    public Map<String, Long> lastDrops = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skyblockextras.json");

    public static SbeConfig load() {
        try {
            if (Files.exists(FILE)) {
                SbeConfig config = GSON.fromJson(Files.readString(FILE), SbeConfig.class);
                if (config != null) {
                    if (config.harvestFeastDrops == null) config.harvestFeastDrops = new LinkedHashMap<>();
                    if (config.farmingDyes == null) config.farmingDyes = new LinkedHashMap<>();
                    if (config.lastDrops == null) config.lastDrops = new LinkedHashMap<>();
                    if (config.rngDropPriceFormat == null) config.rngDropPriceFormat = "SHORT";
                    return config;
                }
            }
        } catch (Exception ignored) { }
        SbeConfig config = new SbeConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException ignored) { }
    }

    public void setLastDrop(String item, long timestamp) {
        if (item == null || item.isBlank()) return;
        if (lastDrops == null) lastDrops = new LinkedHashMap<>();
        lastDrops.put(item, timestamp);
    }

    public Long getLastDrop(String item) {
        if (item == null || item.isBlank()) return null;
        if (lastDrops == null) lastDrops = new LinkedHashMap<>();
        return lastDrops.get(item);
    }

    public void removeLastDrop(String item) {
        if (item == null || item.isBlank()) return;
        if (lastDrops == null) lastDrops = new LinkedHashMap<>();
        lastDrops.remove(item);
    }

    public void clearLastDrops() {
        if (lastDrops == null) lastDrops = new LinkedHashMap<>();
        lastDrops.clear();
    }
}
