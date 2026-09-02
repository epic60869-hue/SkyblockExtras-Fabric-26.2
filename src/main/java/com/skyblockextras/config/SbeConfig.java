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

    public boolean showPetIcon = true;
    public boolean showPetLevel = true;
    public boolean showPetProgress = true;
    public boolean showPetXp = true;
    public boolean showOverflowXp = true;
    public boolean showPetItem = true;

    public float petScale = 1.0f;
    public int petX = 10;
    public int petY = 10;

    // Exact Harvest Feast crop-drop whitelist supplied by the user will be
    // filled in here. Keeping this data-driven makes future edits easy.
    public Map<String, Boolean> harvestFeastDrops = new LinkedHashMap<>();

    public Map<String, Boolean> farmingDyes = new LinkedHashMap<>();
    public boolean epicSlug = true;
    public boolean legendarySlug = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skyblockextras.json");

    public static SbeConfig load() {
        try {
            if (Files.exists(FILE)) {
                SbeConfig c = GSON.fromJson(Files.readString(FILE), SbeConfig.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) {}
        SbeConfig c = new SbeConfig();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException ignored) {}
    }
}
