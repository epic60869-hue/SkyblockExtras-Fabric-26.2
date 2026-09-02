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

import java.util.Map;

public class SbeScreen extends Screen {
    private final Screen parent;
    private String category = "GUI";
    private int left, top, widthBox, heightBox;
    private boolean harvestExpanded = true;
    private boolean slugsExpanded = true;
    private boolean dyesExpanded = false;

    private static final String[] CATEGORIES = {
            "About", "GUI", "Farming RNG", "Inventory", "Chat", "Bazaar", "Hunting", "Pet", "Misc"
    };

    private static final String[] HARVEST_FEAST_DROPS = {
            "Aggourdian", "Botroot", "Cactus Flower", "Cane Knot", "Carrot Zest",
            "Cornucopia", "Cropie", "Crystalized Moonlight", "Deepfries", "Designer Coffee Beans",
            "Feastfungus", "Fermento", "Floral Gelatin", "Helianthus", "Melon Juice",
            "Salted Sunflower Seeds", "Squash"
    };

    public SbeScreen(Screen parent) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        widthBox = Math.min(980, this.width - 50);
        heightBox = Math.min(720, this.height - 40);
        left = (this.width - widthBox) / 2;
        top = (this.height - heightBox) / 2;

        this.addRenderableWidget(new BackgroundWidget(left, top, widthBox, heightBox));

        int categoryY = top + 93;
        for (String name : CATEGORIES) {
            final String selected = name;
            this.addRenderableWidget(Button.builder(Component.literal(name), button -> {
                category = selected;
                rebuildWidgets();
            }).bounds(left + 28, categoryY, 190, 26).build());
            categoryY += 31;
        }

        switch (category) {
            case "GUI" -> addGuiControls();
            case "Farming RNG" -> addFarmingControls();
            case "Pet" -> addPetControls();
            default -> { }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + widthBox - 112, top + heightBox - 35, 90, 24).build());
    }

    private int contentX() {
        return left + 250;
    }

    private int contentW() {
        return widthBox - 275;
    }

    private Button toggleButton(String name, boolean enabled, int x, int y, int w, Runnable action) {
        Button button = Button.builder(toggleText(name, enabled), b -> {
            action.run();
            b.setMessage(toggleText(name, enabledValue(name)));
        }).bounds(x, y, w, 24).build();
        return button;
    }

    private boolean enabledValue(String name) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        return switch (name) {
            case "Pet Overlay" -> c.petOverlayEnabled;
            case "Show Pet Icon" -> c.showPetIcon;
            case "Show Pet Level" -> c.showPetLevel;
            case "Show Pet Progress" -> c.showPetProgress;
            case "Show Pet XP" -> c.showPetXp;
            case "Show Overflow XP" -> c.showOverflowXp;
            case "Show Pet Item" -> c.showPetItem;
            case "Farming RNG" -> c.farmingRngEnabled;
            case "Harvest Feast" -> c.harvestFeastEnabled;
            case "Epic Slug" -> c.epicSlug;
            case "Legendary Slug" -> c.legendarySlug;
            case "Slugs" -> c.slugEnabled;
            case "Farming Dyes" -> c.dyesEnabled;
            default -> false;
        };
    }

    private void addGuiControls() {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int x = contentX() + 22;
        int y = top + 100;
        int w = contentW() - 44;

        addSectionLabel("GUI & HUD", x, y);
        addDescription("Configure overlays and the in-game position editor.", x, y + 20);

        addSettingButton("Position / Size Editor", "Open the editor to drag HUD elements and scroll to resize.", x, y + 52, w,
                Component.literal("Open with /sbe gui"), b -> { });

        addToggle("Pet Overlay", c.petOverlayEnabled, x, y + 113, w, () -> c.petOverlayEnabled = !c.petOverlayEnabled);
        addToggle("Farming RNG", c.farmingRngEnabled, x, y + 145, w, () -> c.farmingRngEnabled = !c.farmingRngEnabled);

        addSectionLabel("INVENTORY", x, y + 194);
        addDescription("Inventory-specific display settings will live here.", x, y + 214);
        addSliderVisual("Inventory GUI Scale", x, y + 247, w, "100%");

        addSectionLabel("KEYBINDS", x, y + 294);
        addDescription("The position editor keybind will be configurable here.", x, y + 314);
    }

    private void addFarmingControls() {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int x = contentX() + 22;
        int y = top + 96;
        int w = contentW() - 44;

        addSectionLabel("FARMING RNG", x, y);
        addDescription("Only enabled items are tracked. Timers are saved between restarts.", x, y + 20);
        addToggle("Farming RNG", c.farmingRngEnabled, x, y + 51, w, () -> c.farmingRngEnabled = !c.farmingRngEnabled);

        int yy = y + 88;
        addCollapsible("Harvest Feast", c.harvestFeastEnabled, harvestExpanded, x, yy, w, () -> {
            harvestExpanded = !harvestExpanded;
            rebuildWidgets();
        }, () -> c.harvestFeastEnabled = !c.harvestFeastEnabled);
        yy += 32;
        if (harvestExpanded) {
            int columns = 2;
            int colW = (w - 8) / columns;
            for (int i = 0; i < HARVEST_FEAST_DROPS.length; i++) {
                String item = HARVEST_FEAST_DROPS[i];
                boolean enabled = c.harvestFeastDrops.getOrDefault(item, true);
                int col = i % columns;
                int row = i / columns;
                addItemToggle(item, enabled, x + col * (colW + 8), yy + row * 29, colW,
                        () -> c.harvestFeastDrops.put(item, !enabled));
            }
            yy += ((HARVEST_FEAST_DROPS.length + 1) / 2) * 29 + 10;
        }

        addCollapsible("Slugs", c.slugEnabled, slugsExpanded, x, yy, w, () -> {
            slugsExpanded = !slugsExpanded;
            rebuildWidgets();
        }, () -> c.slugEnabled = !c.slugEnabled);
        yy += 32;
        if (slugsExpanded) {
            addItemToggle("Epic Slug", c.epicSlug, x, yy, w, () -> c.epicSlug = !c.epicSlug);
            addItemToggle("Legendary Slug", c.legendarySlug, x, yy + 29, w, () -> c.legendarySlug = !c.legendarySlug);
            yy += 68;
        }

        addCollapsible("Farming Dyes", c.dyesEnabled, dyesExpanded, x, yy, w, () -> {
            dyesExpanded = !dyesExpanded;
            rebuildWidgets();
        }, () -> c.dyesEnabled = !c.dyesEnabled);
        yy += 34;
        if (dyesExpanded) {
            if (c.farmingDyes.isEmpty()) {
                addDescription("No farming dyes configured yet.", x + 8, yy);
            } else {
                int i = 0;
                for (Map.Entry<String, Boolean> entry : c.farmingDyes.entrySet()) {
                    final String item = entry.getKey();
                    final boolean enabled = entry.getValue();
                    addItemToggle(item, enabled, x, yy + i * 29, w,
                            () -> c.farmingDyes.put(item, !enabled));
                    i++;
                }
            }
        }
    }

    private void addPetControls() {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int x = contentX() + 22;
        int y = top + 96;
        int w = contentW() - 44;

        addSectionLabel("PET OVERLAY", x, y);
        addDescription("Choose which information is visible in the pet HUD.", x, y + 20);
        addToggle("Pet Overlay", c.petOverlayEnabled, x, y + 51, w, () -> c.petOverlayEnabled = !c.petOverlayEnabled);
        addToggle("Show Pet Icon", c.showPetIcon, x, y + 83, w, () -> c.showPetIcon = !c.showPetIcon);
        addToggle("Show Pet Level", c.showPetLevel, x, y + 115, w, () -> c.showPetLevel = !c.showPetLevel);
        addToggle("Show Pet Progress", c.showPetProgress, x, y + 147, w, () -> c.showPetProgress = !c.showPetProgress);
        addToggle("Show Pet XP", c.showPetXp, x, y + 179, w, () -> c.showPetXp = !c.showPetXp);
        addToggle("Show Overflow XP", c.showOverflowXp, x, y + 211, w, () -> c.showOverflowXp = !c.showOverflowXp);
        addToggle("Show Pet Item", c.showPetItem, x, y + 243, w, () -> c.showPetItem = !c.showPetItem);

        addSectionLabel("SIZE", x, y + 288);
        addSliderVisual("Pet Scale", x, y + 320, w, String.format("%.1fx", c.petScale));
        addDescription("Position: X " + c.petX + "  Y " + c.petY, x, y + 355);
    }

    private void addSettingButton(String title, String description, int x, int y, int w,
                                  Component buttonText, Button.OnPress press) {
        addRenderableWidget(Button.builder(buttonText, press).bounds(x + w - 145, y + 7, 132, 24).build());
        addPanelText(title, description, x, y, w);
    }

    private void addToggle(String name, boolean enabled, int x, int y, int w, Runnable action) {
        Button button = Button.builder(toggleText(name, enabled), b -> {
            action.run();
            b.setMessage(toggleText(name, !enabled));
            SkyblockExtrasClient.CONFIG.save();
        }).bounds(x + w - 165, y, 155, 24).build();
        addRenderableWidget(button);
        addPanelText(name, "Click to toggle this setting.", x, y - 1, w);
    }

    private void addItemToggle(String name, boolean enabled, int x, int y, int w, Runnable action) {
        Button button = Button.builder(toggleText(name, enabled), b -> {
            action.run();
            b.setMessage(toggleText(name, !enabled));
            SkyblockExtrasClient.CONFIG.save();
        }).bounds(x + w - 145, y, 135, 24).build();
        addRenderableWidget(button);
    }

    private void addCollapsible(String name, boolean enabled, boolean expanded, int x, int y, int w,
                                Runnable expandAction, Runnable toggleAction) {
        String arrow = expanded ? "▼ " : "▶ ";
        addRenderableWidget(Button.builder(Component.literal(arrow + name), b -> expandAction.run())
                .bounds(x, y, 150, 26).build());
        addRenderableWidget(Button.builder(toggleText("Enabled", enabled), b -> {
            toggleAction.run();
            b.setMessage(toggleText("Enabled", !enabled));
            SkyblockExtrasClient.CONFIG.save();
        }).bounds(x + w - 120, y + 1, 110, 24).build());
    }

    private void addSliderVisual(String name, int x, int y, int w, String value) {
        addPanelText(name, "", x, y, w);
        addRenderableWidget(Button.builder(Component.literal("◀   " + value + "   ▶"), b -> { })
                .bounds(x + w - 155, y, 145, 24).build());
    }

    private void addSectionLabel(String text, int x, int y) {
        addRenderableWidget(new LabelWidget(x, y, text, 0xFFB47CFF));
    }

    private void addDescription(String text, int x, int y) {
        addRenderableWidget(new LabelWidget(x, y, text, 0xFF9C9CA6));
    }

    private void addPanelText(String title, String description, int x, int y, int w) {
        addRenderableWidget(new PanelWidget(x, y - 2, w, 28, title, description));
    }

    private Component toggleText(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private class BackgroundWidget extends AbstractWidget {
        private BackgroundWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(0, 0, SbeScreen.this.width, SbeScreen.this.height, 0xFF0F0F14);
            graphics.fill(left - 5, top - 5, left + widthBox + 5, top + heightBox + 5, 0xFF101016);
            graphics.outline(left - 5, top - 5, widthBox + 10, heightBox + 10, 0xFF34343E);

            graphics.fill(left, top, left + widthBox, top + 54, 0xFF19191F);
            graphics.outline(left, top, widthBox, 54, 0xFF34343E);

            Component a = Component.literal("Skyblock Extras ").withStyle(ChatFormatting.GRAY);
            Component b = Component.literal("0.1.2").withStyle(ChatFormatting.LIGHT_PURPLE);
            Component c = Component.literal(" — configuration").withStyle(ChatFormatting.GRAY);
            int titleX = left + 25;
            int titleY = top + 19;
            graphics.text(SbeScreen.this.font, a, titleX, titleY, 0xFFFFFFFF, false);
            int tx = titleX + SbeScreen.this.font.width(a);
            graphics.text(SbeScreen.this.font, b, tx, titleY, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, c, tx + SbeScreen.this.font.width(b), titleY, 0xFFFFFFFF, false);

            graphics.fill(left + 15, top + 70, left + 228, top + heightBox - 52, 0xFF15151B);
            graphics.outline(left + 15, top + 70, 213, heightBox - 122, 0xFF34343E);
            graphics.text(SbeScreen.this.font, "Categories", left + 83, top + 83, 0xFFB47CFF, false);

            int selectedIndex = 0;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(category)) {
                    selectedIndex = i;
                    break;
                }
            }
            int highlightY = top + 93 + selectedIndex * 31;
            graphics.fill(left + 20, highlightY - 2, left + 218, highlightY + 26, 0xFF292331);
            graphics.fill(left + 20, highlightY - 2, left + 24, highlightY + 26, 0xFFB45CFF);

            int contentLeft = left + 240;
            graphics.fill(contentLeft, top + 70, left + widthBox - 15, top + heightBox - 52, 0xFF15151B);
            graphics.outline(contentLeft, top + 70, widthBox - 255, heightBox - 122, 0xFF34343E);

            if (category.equals("About")) drawAbout(graphics, contentLeft);
            else if (category.equals("GUI")) drawContentHeader(graphics, contentLeft, "GUI & HUD");
            else if (category.equals("Farming RNG")) drawContentHeader(graphics, contentLeft, "Farming RNG");
            else if (category.equals("Pet")) drawContentHeader(graphics, contentLeft, "Pet Overlay");
            else drawComingSoon(graphics, contentLeft, category);
        }

        private void drawContentHeader(GuiGraphicsExtractor graphics, int x, String title) {
            graphics.text(SbeScreen.this.font, title, x + 20, top + 84, 0xFFFFFFFF, false);
        }

        private void drawAbout(GuiGraphicsExtractor graphics, int x) {
            graphics.text(SbeScreen.this.font, "Skyblock Extras", x + 20, top + 84, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Client-side SkyBlock utilities for Minecraft 26.2.", x + 20, top + 113, 0xFFB0B0B8, false);
            graphics.text(SbeScreen.this.font, "Use the categories to configure HUDs, RNG tracking and more.", x + 20, top + 132, 0xFFB0B0B8, false);
            graphics.text(SbeScreen.this.font, "Version 0.1.2", x + 20, top + 171, 0xFFB47CFF, false);
        }

        private void drawComingSoon(GuiGraphicsExtractor graphics, int x, String title) {
            graphics.text(SbeScreen.this.font, title, x + 20, top + 84, 0xFFFFFFFF, false);
            graphics.text(SbeScreen.this.font, "Settings for this category are coming next.", x + 20, top + 113, 0xFF9C9CA6, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
        }
    }

    private class LabelWidget extends AbstractWidget {
        private final String text;
        private final int color;

        private LabelWidget(int x, int y, String text, int color) {
            super(x, y, Math.max(1, SbeScreen.this.font.width(text) + 2), 18, Component.empty());
            this.text = text;
            this.color = color;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.text(SbeScreen.this.font, text, getX(), getY(), color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }
    }

    private class PanelWidget extends AbstractWidget {
        private final String title;
        private final String description;

        private PanelWidget(int x, int y, int width, int height, String title, String description) {
            super(x, y, width, height, Component.empty());
            this.title = title;
            this.description = description;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF202027);
            graphics.outline(getX(), getY(), width, height, 0xFF30303A);
            graphics.text(SbeScreen.this.font, title, getX() + 10, getY() + 8, 0xFFFFFFFF, false);
            if (!description.isEmpty()) {
                graphics.text(SbeScreen.this.font, description, getX() + 160, getY() + 8, 0xFF9C9CA6, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
