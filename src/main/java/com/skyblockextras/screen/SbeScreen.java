package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SbeScreen extends Screen {

    private final Screen parent;
    private String category = "GUI";

    private int left;
    private int top;
    private int widthBox;
    private int heightBox;

    public SbeScreen(Screen parent) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        this.clearWidgets();

        widthBox = Math.min(930, this.width - 80);
        heightBox = Math.min(700, this.height - 70);
        left = (this.width - widthBox) / 2;
        top = (this.height - heightBox) / 2;

        // Draw the custom GUI first, so the normal Minecraft buttons render on top.
        this.addRenderableWidget(new BackgroundWidget(left - 5, top - 5, widthBox + 10, heightBox + 10));

        String[] categories = {
                "About", "GUI", "Inventory", "Chat", "Bazaar",
                "Hunting", "Pet", "Misc"
        };

        int categoryY = top + 90;
        for (String name : categories) {
            final String selected = name;
            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(name),
                            button -> {
                                category = selected;
                                rebuildWidgets();
                            }
                    ).bounds(left + 25, categoryY, 185, 24).build()
            );
            categoryY += 30;
        }

        if (category.equals("GUI")) {
            addGuiControls();
        } else if (category.equals("Pet")) {
            addPetControls();
        }

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Done"),
                        button -> onClose()
                ).bounds(left + widthBox - 105, top + heightBox - 32, 85, 22).build()
        );
    }

    private void addGuiControls() {
        SbeConfig config = SkyblockExtrasClient.CONFIG;
        int x = left + 315;
        int y = top + 95;

        this.addRenderableWidget(
                Button.builder(Component.literal("Left Arrow"), button -> {})
                        .bounds(x + 30, y + 55, 78, 22).build()
        );

        this.addRenderableWidget(
                Button.builder(
                        toggleText("Separate Inventory GUI Scale", true),
                        button -> {}
                ).bounds(x + 5, y + 145, 230, 22).build()
        );

        this.addRenderableWidget(
                Button.builder(
                        toggleText("Pet Overlay", config.petOverlayEnabled),
                        button -> {
                            config.petOverlayEnabled = !config.petOverlayEnabled;
                            config.save();
                            button.setMessage(toggleText("Pet Overlay", config.petOverlayEnabled));
                        }
                ).bounds(x + 5, y + 215, 230, 22).build()
        );

        this.addRenderableWidget(
                Button.builder(
                        toggleText("Farming RNG", config.farmingRngEnabled),
                        button -> {
                            config.farmingRngEnabled = !config.farmingRngEnabled;
                            config.save();
                            button.setMessage(toggleText("Farming RNG", config.farmingRngEnabled));
                        }
                ).bounds(x + 5, y + 250, 230, 22).build()
        );
    }

    private void addPetControls() {
        SbeConfig config = SkyblockExtrasClient.CONFIG;
        int x = left + 315;
        int y = top + 95;

        this.addRenderableWidget(
                Button.builder(
                        toggleText("Pet Overlay", config.petOverlayEnabled),
                        button -> {
                            config.petOverlayEnabled = !config.petOverlayEnabled;
                            config.save();
                            button.setMessage(toggleText("Pet Overlay", config.petOverlayEnabled));
                        }
                ).bounds(x + 5, y + 55, 230, 22).build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Position / Size Editor"), button -> {})
                        .bounds(x + 5, y + 90, 230, 22).build()
        );
    }

    private Component toggleText(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        // BackgroundWidget contains the custom drawing. Calling super here makes
        // Minecraft extract the background widget and all interactive controls.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private class BackgroundWidget extends AbstractWidget {

        private BackgroundWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float delta
        ) {
            // Main backdrop.
            graphics.fill(0, 0, SbeScreen.this.width, SbeScreen.this.height, 0xFF20202A);

            // Outer frame and shadow.
            graphics.fill(left - 5, top - 5, left + widthBox + 5, top + heightBox + 5, 0xFF111117);
            graphics.outline(left - 5, top - 5, widthBox + 10, heightBox + 10, 0xFF34343E);

            // Header.
            graphics.fill(left, top, left + widthBox, top + 45, 0xFF1B1B21);
            graphics.outline(left, top, widthBox, 45, 0xFF34343E);

            Component title1 = Component.literal("Skyblock Extras ").withStyle(ChatFormatting.GRAY);
            Component title2 = Component.literal("0.1.0").withStyle(ChatFormatting.LIGHT_PURPLE);
            Component title3 = Component.literal(" — configuration").withStyle(ChatFormatting.GRAY);
            int titleX = left + 195;
            graphics.text(SbeScreen.this.font, title1, titleX, top + 14, 0xFFFFFFFF, false);
            int tx = titleX + SbeScreen.this.font.width(title1);
            graphics.text(SbeScreen.this.font, title2, tx, top + 14, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, title3, tx + SbeScreen.this.font.width(title2), top + 14, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "⌕", left + widthBox - 42, top + 10, 0xFFFFFFFF, false);

            // Left navigation panel.
            graphics.fill(left + 15, top + 55, left + 215, top + heightBox - 15, 0xFF17171D);
            graphics.outline(left + 15, top + 55, 200, heightBox - 70, 0xFF34343E);
            graphics.text(SbeScreen.this.font, "Categories", left + 74, top + 68, 0xFFB47CFF, false);

            int selectedIndex = switch (category) {
                case "About" -> 0;
                case "GUI" -> 1;
                case "Inventory" -> 2;
                case "Chat" -> 3;
                case "Bazaar" -> 4;
                case "Hunting" -> 5;
                case "Pet" -> 6;
                default -> 7;
            };
            int highlightY = top + 90 + selectedIndex * 30;
            graphics.fill(left + 20, highlightY - 1, left + 205, highlightY + 23, 0xFF2A2635);
            graphics.fill(left + 20, highlightY - 1, left + 23, highlightY + 23, 0xFFB45CFF);

            // Main content panel.
            int contentLeft = left + 225;
            graphics.fill(contentLeft, top + 55, left + widthBox - 10, top + heightBox - 15, 0xFF17171D);
            graphics.outline(contentLeft, top + 55, widthBox - 245, heightBox - 70, 0xFF34343E);

            if (category.equals("GUI")) {
                drawGuiPage(graphics, contentLeft);
            } else if (category.equals("Pet")) {
                drawPetPage(graphics, contentLeft);
            } else if (category.equals("About")) {
                graphics.text(SbeScreen.this.font, "Skyblock Extras", contentLeft + 18, top + 75, 0xFFFFFFFF, false);
                graphics.text(SbeScreen.this.font, "A client-side SkyBlock utility mod.", contentLeft + 18, top + 105, 0xFFB0B0B8, false);
                graphics.text(SbeScreen.this.font, "Use the categories on the left to configure SBE.", contentLeft + 18, top + 125, 0xFFB0B0B8, false);
            } else {
                graphics.text(SbeScreen.this.font, category, contentLeft + 18, top + 75, 0xFFFFFFFF, false);
                graphics.text(SbeScreen.this.font, "Settings for this category are coming next.", contentLeft + 18, top + 105, 0xFFB0B0B8, false);
            }
        }

        private void drawGuiPage(GuiGraphicsExtractor graphics, int contentLeft) {
            int x = contentLeft + 18;
            int y = top + 72;
            graphics.text(SbeScreen.this.font, "GUI and HUD editor settings.", x, y, 0xFFFFFFFF, false);

            drawPanel(graphics, x, y + 28, 560, 75);
            graphics.text(SbeScreen.this.font, "Position Editor Keybind", x + 10, y + 43, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Press this key to open the Position Editor.", x + 205, y + 43, 0xFFB0B0B8, false);

            drawPanel(graphics, x, y + 112, 560, 70);
            graphics.text(SbeScreen.this.font, "Inventory Screen", x + 10, y + 126, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Separate Inventory GUI Scale", x + 10, y + 151, 0xFFB0B0B8, false);
            graphics.text(SbeScreen.this.font, "Inventory GUI scale", x + 10, y + 169, 0xFFB0B0B8, false);

            drawPanel(graphics, x, y + 191, 560, 70);
            graphics.text(SbeScreen.this.font, "Pet Overlay", x + 10, y + 205, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Pet information overlay and positioning.", x + 10, y + 230, 0xFFB0B0B8, false);

            drawPanel(graphics, x, y + 270, 560, 70);
            graphics.text(SbeScreen.this.font, "Farming RNG", x + 10, y + 284, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Track rare farming drops in chat with persistent timers.", x + 10, y + 309, 0xFFB0B0B8, false);
        }

        private void drawPetPage(GuiGraphicsExtractor graphics, int contentLeft) {
            int x = contentLeft + 18;
            int y = top + 72;
            graphics.text(SbeScreen.this.font, "Pet Overlay", x, y, 0xFFFFFFFF, false);
            drawPanel(graphics, x, y + 28, 560, 95);
            graphics.text(SbeScreen.this.font, "Pet overlay", x + 10, y + 43, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Shows pet level, XP, overflow XP and held pet item.", x + 10, y + 68, 0xFFB0B0B8, false);
            graphics.text(SbeScreen.this.font, "Use Position / Size Editor to move and resize it.", x + 10, y + 88, 0xFFB0B0B8, false);
        }

        private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
            graphics.fill(x, y, x + w, y + h, 0xFF202027);
            graphics.outline(x, y, w, h, 0xFF30303A);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            // Decorative background; no narration needed.
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
