package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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

        // Left category navigation.
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

        // Content controls depend on the selected category.
        if (category.equals("GUI")) {
            addGuiControls();
        } else if (category.equals("Pet")) {
            addPetControls();
        } else if (category.equals("About")) {
            // No controls on the About page.
        } else {
            addPlaceholderControls();
        }

        // Done button.
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

        // Position editor key.
        this.addRenderableWidget(
                Button.builder(Component.literal("Left Arrow"), button -> {})
                        .bounds(x + 30, y + 55, 78, 22).build()
        );

        // Inventory GUI scale toggle.
        addToggle(x + 5, y + 145, "Separate Inventory GUI Scale", true);

        // Pet overlay toggle.
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

        // Farming RNG master toggle.
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

    private void addPlaceholderControls() {
        // Keep the navigation functional while the individual pages are implemented.
        // The page itself is drawn in extractRenderState().
    }

    private void addToggle(int x, int y, String name, boolean enabled) {
        this.addRenderableWidget(
                Button.builder(
                        toggleText(name, enabled),
                        button -> button.setMessage(Component.literal(name + ": ON"))
                ).bounds(x, y, 230, 22).build()
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
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Main background and outer frame.
        graphics.fill(0, 0, this.width, this.height, 0xFF20202A);

        // Shadow / outer border.
        graphics.fill(left - 5, top - 5, left + widthBox + 5, top + heightBox + 5, 0xFF111117);
        graphics.outline(left - 5, top - 5, widthBox + 10, heightBox + 10, 0xFF34343E);

        // Header.
        graphics.fill(left, top, left + widthBox, top + 45, 0xFF1B1B21);
        graphics.outline(left, top, widthBox, 45, 0xFF34343E);

        int titleX = left + 195;
        Component title1 = Component.literal("Skyblock Extras ").withStyle(ChatFormatting.GRAY);
        Component title2 = Component.literal("0.1.0").withStyle(ChatFormatting.LIGHT_PURPLE);
        Component title3 = Component.literal(" — configuration").withStyle(ChatFormatting.GRAY);
        graphics.text(this.font, title1, titleX, top + 14, 0xFFFFFFFF, false);
        int tx = titleX + this.font.width(title1);
        graphics.text(this.font, title2, tx, top + 14, 0xFFFFFFFF, false);
        graphics.text(this.font, title3, tx + this.font.width(title2), top + 14, 0xFFFFFFFF, false);

        // Magnifying-glass style glyph.
        graphics.text(this.font, "⌕", left + widthBox - 42, top + 10, 0xFFFFFFFF, false);

        // Left navigation panel.
        graphics.fill(left + 15, top + 55, left + 215, top + heightBox - 15, 0xFF17171D);
        graphics.outline(left + 15, top + 55, 200, heightBox - 70, 0xFF34343E);
        graphics.text(this.font, "Categories", left + 74, top + 68, 0xFFB47CFF, false);

        // Highlight selected category behind its button.
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
            graphics.text(this.font, "Skyblock Extras", contentLeft + 18, top + 75, 0xFFFFFFFF, false);
            graphics.text(this.font, "A client-side SkyBlock utility mod.", contentLeft + 18, top + 105, 0xFFB0B0B8, false);
            graphics.text(this.font, "Use the categories on the left to configure SBE.", contentLeft + 18, top + 125, 0xFFB0B0B8, false);
        } else {
            graphics.text(this.font, category, contentLeft + 18, top + 75, 0xFFFFFFFF, false);
            graphics.text(this.font, "Settings for this category are coming next.", contentLeft + 18, top + 105, 0xFFB0B0B8, false);
        }
    }

    private void drawGuiPage(GuiGraphicsExtractor graphics, int contentLeft) {
        int x = contentLeft + 18;
        int y = top + 72;
        graphics.text(this.font, "GUI and HUD editor settings.", x, y, 0xFFFFFFFF, false);

        drawPanel(graphics, x, y + 28, 560, 75);
        graphics.text(this.font, "Position Editor Keybind", x + 10, y + 43, 0xFFFFFFFF, false);
        graphics.text(this.font, "Press this key to open the Position Editor.", x + 205, y + 43, 0xFFB0B0B8, false);

        drawPanel(graphics, x, y + 112, 560, 70);
        graphics.text(this.font, "Inventory Screen", x + 10, y + 126, 0xFFFFFFFF, false);
        graphics.text(this.font, "Separate Inventory GUI Scale", x + 10, y + 151, 0xFFB0B0B8, false);
        graphics.text(this.font, "Inventory GUI scale", x + 10, y + 169, 0xFFB0B0B8, false);

        drawPanel(graphics, x, y + 191, 560, 70);
        graphics.text(this.font, "Pet Overlay", x + 10, y + 205, 0xFFFFFFFF, false);
        graphics.text(this.font, "Pet information overlay and positioning.", x + 10, y + 230, 0xFFB0B0B8, false);

        drawPanel(graphics, x, y + 270, 560, 70);
        graphics.text(this.font, "Farming RNG", x + 10, y + 284, 0xFFFFFFFF, false);
        graphics.text(this.font, "Track rare farming drops in chat with persistent timers.", x + 10, y + 309, 0xFFB0B0B8, false);
    }

    private void drawPetPage(GuiGraphicsExtractor graphics, int contentLeft) {
        int x = contentLeft + 18;
        int y = top + 72;
        graphics.text(this.font, "Pet Overlay", x, y, 0xFFFFFFFF, false);
        drawPanel(graphics, x, y + 28, 560, 95);
        graphics.text(this.font, "Pet overlay", x + 10, y + 43, 0xFFFFFFFF, false);
        graphics.text(this.font, "Shows pet level, XP, overflow XP and held pet item.", x + 10, y + 68, 0xFFB0B0B8, false);
        graphics.text(this.font, "Use Position / Size Editor to move and resize it.", x + 10, y + 88, 0xFFB0B0B8, false);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF202027);
        graphics.outline(x, y, w, h, 0xFF30303A);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
