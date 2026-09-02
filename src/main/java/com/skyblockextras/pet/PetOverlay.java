package com.skyblockextras.pet;

import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;

public class PetOverlay {

    private final SbeConfig config;

    /*
     * Placeholder pet information.
     *
     * These values will later be populated from the
     * actual SkyBlock pet data.
     */
    private String petName = "Golden Dragon";
    private String petRarity = "LEGENDARY";

    private int petLevel = 100;

    private long currentXp = 0;
    private long requiredXp = 0;
    private long overflowXp = 0;

    private String petItem = "";


    public PetOverlay(SbeConfig config) {
        this.config = config;
    }


    // ============================================================
    // HUD RENDERING
    // ============================================================

    public void render(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {

        if (!config.petOverlayEnabled) {
            return;
        }

        Minecraft client =
                Minecraft.getInstance();

        if (client.player == null) {
            return;
        }


        int x = config.petX;
        int y = config.petY;


        /*
         * Use the configured scale.
         */
        float scale = config.petScale;

        if (scale <= 0.0f) {
            scale = 1.0f;
        }


        /*
         * Save the matrix state before applying our scale.
         */
        var matrices = graphics.pose();

        matrices.pushMatrix();

        matrices.translate(
                x,
                y
        );

        matrices.scale(
                scale,
                scale
        );


        int drawX = 0;
        int drawY = 0;


        // ========================================================
        // BACKGROUND
        // ========================================================

        /*
         * Small transparent-style background.
         *
         * This gives the overlay a clean SkyBlock utility
         * appearance without taking up too much space.
         */
        int width = getOverlayWidth();
        int height = getOverlayHeight();

        graphics.fill(
                drawX - 3,
                drawY - 3,
                drawX + width + 3,
                drawY + height + 3,
                0x90000000
        );


        // ========================================================
        // PET ICON
        // ========================================================

        int textX = drawX;

        if (config.showPetIcon) {

            /*
             * Until the actual pet texture/item system is added,
             * use a small placeholder square.
             */
            graphics.fill(
                    drawX,
                    drawY,
                    drawX + 16,
                    drawY + 16,
                    0xFF555555
            );

            graphics.outline(
                    drawX,
                    drawY,
                    16,
                    16,
                    0xFFFFFFFF
            );

            textX += 20;
        }


        // ========================================================
        // PET NAME / LEVEL
        // ========================================================

        StringBuilder nameLine =
                new StringBuilder();


        if (config.showPetLevel) {

            nameLine.append(
                    "[Lvl "
            );

            nameLine.append(
                    petLevel
            );

            nameLine.append(
                    "] "
            );
        }


        nameLine.append(
                petName
        );


        if (petRarity != null &&
                !petRarity.isBlank()) {

            nameLine.append(
                    " "
            );

            nameLine.append(
                    petRarity
            );
        }


        graphics.text(
                client.font,
                Component.literal(
                        nameLine.toString()
                ),
                textX,
                drawY,
                0xFFFFFFFF,
                true
        );


        int nextY = drawY + 12;


        // ========================================================
        // XP PROGRESS
        // ========================================================

        if (config.showPetProgress) {

            int barWidth = 120;
            int barHeight = 5;

            float progress = getProgress();

            /*
             * Background.
             */
            graphics.fill(
                    textX,
                    nextY,
                    textX + barWidth,
                    nextY + barHeight,
                    0xFF333333
            );


            /*
             * Progress.
             */
            int progressWidth =
                    (int) (
                            barWidth *
                            progress
                    );

            if (progressWidth > 0) {

                graphics.fill(
                        textX,
                        nextY,
                        textX + progressWidth,
                        nextY + barHeight,
                        0xFF55FF55
                );
            }


            nextY += 8;
        }


        // ========================================================
        // CURRENT XP
        // ========================================================

        if (config.showPetXp) {

            graphics.text(
                    client.font,
                    Component.literal(
                            "XP: "
                                    + formatNumber(
                                    currentXp
                            )
                    ),
                    textX,
                    nextY,
                    0xFFFFFFFF,
                    true
            );

            nextY += 10;
        }


        // ========================================================
        // OVERFLOW XP
        // ========================================================

        if (config.showOverflowXp) {

            graphics.text(
                    client.font,
                    Component.literal(
                            "Overflow: "
                                    + formatNumber(
                                    overflowXp
                            )
                    ),
                    textX,
                    nextY,
                    0xFFFFAA00,
                    true
            );

            nextY += 10;
        }


        // ========================================================
        // PET ITEM
        // ========================================================

        if (config.showPetItem &&
                petItem != null &&
                !petItem.isBlank()) {

            graphics.text(
                    client.font,
                    Component.literal(
                            petItem
                    ),
                    textX,
                    nextY,
                    0xFFAAAAAA,
                    true
            );
        }


        /*
         * Restore the matrix.
         */
        matrices.popMatrix();
    }


    // ============================================================
    // PROGRESS
    // ============================================================

    private float getProgress() {

        if (requiredXp <= 0) {
            return 0.0f;
        }

        float progress =
                (float) currentXp /
                (float) requiredXp;

        if (progress < 0.0f) {
            return 0.0f;
        }

        if (progress > 1.0f) {
            return 1.0f;
        }

        return progress;
    }


    // ============================================================
    // OVERLAY SIZE
    // ============================================================

    private int getOverlayWidth() {

        return 150;
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
                petItem != null &&
                !petItem.isBlank()) {

            height += 10;
        }

        return height;
    }


    // ============================================================
    // NUMBER FORMAT
    // ============================================================

    private String formatNumber(
            long number
    ) {

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
    // PET DATA
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

        petLevel = level;
    }


    public void setXp(
            long current,
            long required,
            long overflow
    ) {

        currentXp = Math.max(
                0,
                current
        );

        requiredXp = Math.max(
                0,
                required
        );

        overflowXp = Math.max(
                0,
                overflow
        );
    }


    public void setPetItem(
            String item
    ) {

        petItem =
                item == null
                        ? ""
                        : item;
    }
}
