package com.skyblockextras.pet;

import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class PetOverlay {

    private final SbeConfig config;

    // Current pet data
    private String petName = "No Pet";
    private String petRarity = "";
    private int petLevel = 1;

    private long currentXp = 0L;
    private long requiredXp = 0L;
    private long overflowXp = 0L;

    private String petItem = "";

    // Used to avoid unnecessary updates
    private String lastDetectedPet = "";


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public PetOverlay(SbeConfig config) {
        this.config = config;
    }


    // ============================================================
    // TICK
    // ============================================================

    public void tick(Minecraft client) {

        if (!config.petOverlayEnabled) {
            return;
        }

        if (client == null || client.player == null) {
            return;
        }

        /*
         * Pet detection will be connected here.
         *
         * We deliberately don't guess pet information from random
         * chat messages. The actual SkyBlock pet detection can be
         * added without changing the rendering code below.
         */
    }


    // ============================================================
    // HUD RENDER
    // ============================================================

    public void render(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {

        if (!config.petOverlayEnabled) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        float scale = config.petScale;

        if (scale <= 0.0f) {
            scale = 1.0f;
        }

        int x = config.petX;
        int y = config.petY;

        var pose = graphics.pose();

        pose.pushMatrix();

        pose.translate(
                x,
                y
        );

        pose.scale(
                scale,
                scale
        );


        // ========================================================
        // CALCULATE SIZE
        // ========================================================

        int width = getOverlayWidth();
        int height = getOverlayHeight();


        // ========================================================
        // BACKGROUND
        // ========================================================

        graphics.fill(
                -4,
                -4,
                width + 4,
                height + 4,
                0xB0000000
        );


        // ========================================================
        // PET ICON
        // ========================================================

        int textX = 0;

        if (config.showPetIcon) {

            /*
             * Temporary icon placeholder.
             *
             * The actual SkyBlock pet item/icon will be connected
             * once pet detection is implemented.
             */
            graphics.fill(
                    0,
                    0,
                    16,
                    16,
                    0xFF444444
            );

            graphics.outline(
                    0,
                    0,
                    16,
                    16,
                    0xFFFFFFFF
            );

            textX = 20;
        }


        // ========================================================
        // PET NAME
        // ========================================================

        StringBuilder name = new StringBuilder();

        if (config.showPetLevel) {

            name.append("[Lvl ");
            name.append(petLevel);
            name.append("] ");
        }

        name.append(petName);

        if (!petRarity.isBlank()) {

            name.append(" ");
            name.append(petRarity);
        }


        graphics.text(
                client.font,
                Component.literal(name.toString()),
                textX,
                0,
                0xFFFFFFFF,
                true
        );


        int currentY = 12;


        // ========================================================
        // XP PROGRESS BAR
        // ========================================================

        if (config.showPetProgress) {

            int barWidth = 125;
            int barHeight = 5;

            float progress = getProgress();

            // Background
            graphics.fill(
                    textX,
                    currentY,
                    textX + barWidth,
                    currentY + barHeight,
                    0xFF333333
            );

            // Progress
            int progressWidth =
                    Math.round(
                            barWidth * progress
                    );

            if (progressWidth > 0) {

                graphics.fill(
                        textX,
                        currentY,
                        textX + progressWidth,
                        currentY + barHeight,
                        0xFF55FF55
                );
            }

            currentY += 8;
        }


        // ========================================================
        // CURRENT XP
        // ========================================================

        if (config.showPetXp) {

            graphics.text(
                    client.font,
                    Component.literal(
                            "XP: "
                                    + formatNumber(currentXp)
                    ),
                    textX,
                    currentY,
                    0xFFFFFFFF,
                    true
            );

            currentY += 10;
        }


        // ========================================================
        // OVERFLOW XP
        // ========================================================

        if (config.showOverflowXp) {

            graphics.text(
                    client.font,
                    Component.literal(
                            "Overflow: "
                                    + formatNumber(overflowXp)
                    ),
                    textX,
                    currentY,
                    0xFFFFAA00,
                    true
            );

            currentY += 10;
        }


        // ========================================================
        // PET ITEM
        // ========================================================

        if (config.showPetItem &&
                !petItem.isBlank()) {

            graphics.text(
                    client.font,
                    Component.literal(petItem),
                    textX,
                    currentY,
                    0xFFAAAAAA,
                    true
            );
        }


        pose.popMatrix();
    }


    // ============================================================
    // XP PROGRESS
    // ============================================================

    private float getProgress() {

        if (requiredXp <= 0L) {
            return 0.0f;
        }

        float progress =
                (float) currentXp /
                (float) requiredXp;

        return Math.max(
                0.0f,
                Math.min(
                        1.0f,
                        progress
                )
        );
    }


    // ============================================================
    // OVERLAY SIZE
    // ============================================================

    private int getOverlayWidth() {

        int width = 125;

        if (config.showPetIcon) {
            width += 20;
        }

        return width;
    }


    private int getOverlayHeight() {

        int height = 18;

        if (config.showPetProgress) {
            height += 8;
        }

        if (config.showPetXp) {
            height += 10;
        }

        if (config.showOverflowXp) {
            height += 10;
        }

        if (config.showPetItem &&
                !petItem.isBlank()) {

            height += 10;
        }

        return height;
    }


    // ============================================================
    // NUMBER FORMAT
    // ============================================================

    private String formatNumber(long number) {

        if (number >= 1_000_000_000L) {

            return String.format(
                    "%.2fB",
                    number / 1_000_000_000.0
            );
        }

        if (number >= 1_000_000L) {

            return String.format(
                    "%.2fM",
                    number / 1_000_000.0
            );
        }

        if (number >= 1_000L) {

            return String.format(
                    "%.2fK",
                    number / 1_000.0
            );
        }

        return Long.toString(number);
    }


    // ============================================================
    // SET PET
    // ============================================================

    public void setPet(
            String name,
            String rarity,
            int level
    ) {

        if (name != null &&
                !name.isBlank()) {

            petName = name;
        }

        if (rarity != null &&
                !rarity.isBlank()) {

            petRarity = rarity;
        }

        petLevel = Math.max(
                1,
                level
        );


        lastDetectedPet =
                petName;
    }


    // ============================================================
    // SET XP
    // ============================================================

    public void setXp(
            long current,
            long required,
            long overflow
    ) {

        currentXp =
                Math.max(
                        0L,
                        current
                );

        requiredXp =
                Math.max(
                        0L,
                        required
                );

        overflowXp =
                Math.max(
                        0L,
                        overflow
                );
    }


    // ============================================================
    // SET PET ITEM
    // ============================================================

    public void setPetItem(String item) {

        petItem =
                item == null
                        ? ""
                        : item;
    }


    // ============================================================
    // CLEAR PET
    // ============================================================

    public void clearPet() {

        petName = "No Pet";
        petRarity = "";
        petLevel = 1;

        currentXp = 0L;
        requiredXp = 0L;
        overflowXp = 0L;

        petItem = "";

        lastDetectedPet = "";
    }


    // ============================================================
    // GETTERS
    // ============================================================

    public String getPetName() {
        return petName;
    }

    public String getPetRarity() {
        return petRarity;
    }

    public int getPetLevel() {
        return petLevel;
    }

    public long getCurrentXp() {
        return currentXp;
    }

    public long getRequiredXp() {
        return requiredXp;
    }

    public long getOverflowXp() {
        return overflowXp;
    }

    public String getPetItem() {
        return petItem;
    }
}
