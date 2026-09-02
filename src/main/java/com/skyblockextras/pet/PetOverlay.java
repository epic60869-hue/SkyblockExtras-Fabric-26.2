package com.skyblockextras.pet;

import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;

public class PetOverlay {
    private final SbeConfig config;

    public PetOverlay(SbeConfig config) {
        this.config = config;
    }

    public void tick(Minecraft client) {
        // Placeholder for Hypixel pet-data parsing.
        // The renderer/editor is deliberately isolated so the pet parser can
        // be expanded without changing the GUI code.
    }

    public void render(DrawContext context) {
        if (!config.petOverlayEnabled) return;
        // Render implementation is intentionally a framework stub for now.
        // Pet data should be populated from Hypixel's visible pet/XP data.
    }
}
