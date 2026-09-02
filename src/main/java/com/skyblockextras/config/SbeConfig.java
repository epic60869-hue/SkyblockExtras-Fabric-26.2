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

    // ============================================================
    // MAIN SETTINGS
    // ============================================================

    public boolean farmingRngEnabled = true;

    public boolean harvestFeastEnabled = true;
    public boolean slugEnabled = true;
    public boolean dyesEnabled = true;

    public boolean petOverlayEnabled = true;


    // ============================================================
    // PET OVERLAY SETTINGS
    // ============================================================

    public boolean showPetIcon = true;
    public boolean showPetLevel = true;
    public boolean showPetProgress = true;
    public boolean showPetXp = true;
    public boolean showOverflowXp = true;
    public boolean showPetItem = true;

    public float petScale = 1.0f;

    public int petX = 10;
    public int petY = 10;


    // ============================================================
    // HARVEST FEAST
    // ============================================================

    /*
     * Only items placed in this map will be tracked.
     *
     * Example:
     * "Item Name": true
     */
    public Map<String, Boolean> harvestFeastDrops = new LinkedHashMap<>();


    // ============================================================
    // FARMING DYES
    // ============================================================

    public Map<String, Boolean> farmingDyes = new LinkedHashMap<>();


    // ============================================================
    // SLUG RNG
    // ============================================================

    public boolean epicSlug = true;
    public boolean legendarySlug = true;


    // ============================================================
    // PERSISTENT RNG TIMESTAMPS
    // ============================================================

    /*
     * Stores the last time each tracked RNG dropped.
     *
     * Key:
     *     Item name
     *
     * Value:
     *     Unix timestamp in milliseconds
     *
     * Example:
     *     "Epic Slug": 1756830000000
     */
    public Map<String, Long> lastDrops = new LinkedHashMap<>();


    // ============================================================
    // FILE
    // ============================================================

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("skyblockextras.json");


    // ============================================================
    // LOAD
    // ============================================================

    public static SbeConfig load() {

        try {

            if (Files.exists(FILE)) {

                SbeConfig config =
                        GSON.fromJson(
                                Files.readString(FILE),
                                SbeConfig.class
                        );

                if (config != null) {

                    // Protect against old config files that don't
                    // contain the newer maps.

                    if (config.harvestFeastDrops == null) {
                        config.harvestFeastDrops =
                                new LinkedHashMap<>();
                    }

                    if (config.farmingDyes == null) {
                        config.farmingDyes =
                                new LinkedHashMap<>();
                    }

                    if (config.lastDrops == null) {
                        config.lastDrops =
                                new LinkedHashMap<>();
                    }

                    return config;
                }
            }

        } catch (Exception ignored) {
        }


        // No config exists, so create a fresh one.

        SbeConfig config = new SbeConfig();

        config.save();

        return config;
    }


    // ============================================================
    // SAVE
    // ============================================================

    public void save() {

        try {

            Files.createDirectories(
                    FILE.getParent()
            );

            Files.writeString(
                    FILE,
                    GSON.toJson(this)
            );

        } catch (IOException ignored) {
        }
    }


    // ============================================================
    // RNG TIMESTAMP METHODS
    // ============================================================

    /**
     * Records the timestamp of the latest drop.
     */
    public void setLastDrop(String item, long timestamp) {

        if (item == null || item.isBlank()) {
            return;
        }

        if (lastDrops == null) {
            lastDrops = new LinkedHashMap<>();
        }

        lastDrops.put(item, timestamp);
    }


    /**
     * Gets the timestamp of the latest drop.
     *
     * Returns null if the item has never dropped.
     */
    public Long getLastDrop(String item) {

        if (item == null || item.isBlank()) {
            return null;
        }

        if (lastDrops == null) {
            lastDrops = new LinkedHashMap<>();
        }

        return lastDrops.get(item);
    }


    /**
     * Removes the timer for one RNG.
     */
    public void removeLastDrop(String item) {

        if (item == null || item.isBlank()) {
            return;
        }

        if (lastDrops == null) {
            lastDrops = new LinkedHashMap<>();
        }

        lastDrops.remove(item);
    }


    /**
     * Clears every stored RNG timer.
     */
    public void clearLastDrops() {

        if (lastDrops == null) {
            lastDrops = new LinkedHashMap<>();
        }

        lastDrops.clear();
    }
}
