package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SbeScreen extends Screen {

    private final Screen parent;

    public SbeScreen(Screen parent) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        SbeConfig config = SkyblockExtrasClient.CONFIG;

        int centerX = this.width / 2;
        int y = 45;

        // =========================================================
        // FARMING RNG
        // =========================================================

        this.addRenderableWidget(
                Button.builder(
                        toggleText(
                                "Farming RNG",
                                config.farmingRngEnabled
                        ),
                        button -> {
                            config.farmingRngEnabled =
                                    !config.farmingRngEnabled;

                            config.save();

                            button.setMessage(
                                    toggleText(
                                            "Farming RNG",
                                            config.farmingRngEnabled
                                    )
                            );
                        }
                ).bounds(centerX - 100, y, 200, 20).build()
        );

        y += 25;

        // =========================================================
        // HARVEST FEAST
        // =========================================================

        this.addRenderableWidget(
                Button.builder(
                        toggleText(
                                "Harvest Feast",
                                config.harvestFeastEnabled
                        ),
                        button -> {
                            config.harvestFeastEnabled =
                                    !config.harvestFeastEnabled;

                            config.save();

                            button.setMessage(
                                    toggleText(
                                            "Harvest Feast",
                                            config.harvestFeastEnabled
                                    )
                            );
                        }
                ).bounds(centerX - 100, y, 200, 20).build()
        );

        y += 25;

        // =========================================================
        // SLUGS
        // =========================================================

        this.addRenderableWidget(
                Button.builder(
                        toggleText(
                                "Slugs",
                                config.slugEnabled
                        ),
                        button -> {
                            config.slugEnabled =
                                    !config.slugEnabled;

                            config.save();

                            button.setMessage(
                                    toggleText(
                                            "Slugs",
                                            config.slugEnabled
                                    )
                            );
                        }
                ).bounds(centerX - 100, y, 200, 20).build()
        );

        y += 25;

        // =========================================================
        // FARMING DYES
        // =========================================================

        this.addRenderableWidget(
                Button.builder(
                        toggleText(
                                "Farming Dyes",
                                config.dyesEnabled
                        ),
                        button -> {
                            config.dyesEnabled =
                                    !config.dyesEnabled;

                            config.save();

                            button.setMessage(
                                    toggleText(
                                            "Farming Dyes",
                                            config.dyesEnabled
                                    )
                            );
                        }
                ).bounds(centerX - 100, y, 200, 20).build()
        );

        y += 25;

        // =========================================================
        // PET OVERLAY
        // =========================================================

        this.addRenderableWidget(
                Button.builder(
                        toggleText(
                                "Pet Overlay",
                                config.petOverlayEnabled
                        ),
                        button -> {
                            config.petOverlayEnabled =
                                    !config.petOverlayEnabled;

                            config.save();

                            button.setMessage(
                                    toggleText(
                                            "Pet Overlay",
                                            config.petOverlayEnabled
                                    )
                            );
                        }
                ).bounds(centerX - 100, y, 200, 20).build()
        );

        y += 35;

        // =========================================================
        // DONE
        // =========================================================

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Done"),
                        button -> onClose()
                ).bounds(centerX - 100, y, 200, 20).build()
        );
    }

    // =============================================================
    // MINECRAFT 26.2 GUI RENDERING
    // =============================================================

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        // This is REQUIRED so Minecraft renders the background
        // and all widgets registered in init().
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        // Draw the title.
        graphics.text(
                this.font,
                this.title,
                this.width / 2 - this.font.width(this.title) / 2,
                20,
                0xFFFFFFFF,
                true
        );
    }

    // =============================================================
    // BUTTON TEXT
    // =============================================================

    private Component toggleText(
            String name,
            boolean enabled
    ) {
        return Component.literal(
                name + ": " + (enabled ? "ON" : "OFF")
        );
    }

    // =============================================================
    // CLOSE
    // =============================================================

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
