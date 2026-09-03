package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** A Java/Fabric 26.2 implementation of the clean Skysoft-style config layout. */
public class SbeModernScreen extends Screen {
    private final Screen parent;
    private final String category;
    private int left, top, panelWidth, panelHeight;

    private static final String[] CATEGORIES = {"About", "GUI", "Farming RNG", "Pet"};
    private static final String[] HARVEST = {
            "Aggourdian", "Botroot", "Cactus Flower", "Cane Knot", "Carrot Zest", "Cornucopia", "Cropie",
            "Crystalized Moonlight", "Deepfries", "Designer Coffee Beans", "Feastfungus", "Fermento",
            "Floral Gelatin", "Helianthus", "Melon Juice", "Salted Sunflower Seeds", "Squash"
    };

    public SbeModernScreen(Screen parent) { this(parent, "About"); }
    public SbeModernScreen(Screen parent, String category) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
        this.category = valid(category) ? category : "About";
    }

    private static boolean valid(String value) {
        for (String c : CATEGORIES) if (c.equals(value)) return true;
        return false;
    }

    @Override protected void init() {
        super.init();
        panelWidth = Math.min(960, width - 30);
        panelHeight = Math.min(700, height - 30);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        addRenderableWidget(new PaintWidget(left, top, panelWidth, panelHeight, 0));

        int sideX = left + 12, sideY = top + 58, sideW = 225, sideH = panelHeight - 72;
        addRenderableWidget(new PaintWidget(sideX, sideY, sideW, sideH, 1));
        addLabel("Categories", sideX + 12, sideY + 13, 0xFFB96BFF);
        for (int i = 0; i < CATEGORIES.length; i++) {
            String selected = CATEGORIES[i];
            addRenderableWidget(new NavButton(sideX + 10, sideY + 43 + i * 39, sideW - 20, 31, selected,
                    selected.equals(category), () -> Minecraft.getInstance().gui.setScreen(new SbeModernScreen(parent, selected))));
        }

        int contentX = left + 247, contentY = top + 58, contentW = panelWidth - 259, contentH = panelHeight - 72;
        addRenderableWidget(new PaintWidget(contentX, contentY, contentW, contentH, 1));
        switch (category) {
            case "GUI" -> buildGui(contentX, contentY, contentW);
            case "Farming RNG" -> buildFarming(contentX, contentY, contentW);
            case "Pet" -> buildPet(contentX, contentY, contentW);
            default -> buildAbout(contentX, contentY, contentW);
        }
        addRenderableWidget(new ActionButton(left + panelWidth - 105, top + panelHeight - 39, 88, 26, "Done", this::onClose));
    }

    private void buildAbout(int x, int y, int w) {
        addHeader(x, y, "About", "Skyblock Extras client-side utilities for Minecraft 26.2.");
        addCard(x + 14, y + 72, w - 28, 88, "Skyblock Extras 0.1.2", "A lightweight SkyBlock HUD and RNG utility mod.");
        addCard(x + 14, y + 172, w - 28, 88, "Inspired by modern SkyBlock interfaces", "Clean panels, compact controls and an easy HUD editor.");
    }

    private void buildGui(int x, int y, int w) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        addHeader(x, y, "GUI", "Configure HUD visibility, position and scaling.");
        int cy = y + 68;
        addToggleCard(x + 14, cy, w - 28, "Pet Overlay", "Show the active pet HUD.", () -> c.petOverlayEnabled,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled)); cy += 55;
        addToggleCard(x + 14, cy, w - 28, "RNG Drop Overlay", "Show a centered announcement when a tracked RNG drop is found.", () -> c.rngDropOverlayEnabled,
                () -> toggle(() -> c.rngDropOverlayEnabled = !c.rngDropOverlayEnabled)); cy += 55;
        addButtonCard(x + 14, cy, w - 28, "Pet Position", "Drag and resize the pet HUD.", "OPEN EDITOR",
                () -> Minecraft.getInstance().gui.setScreen(new PositionEditorScreen(this))); cy += 68;
        addButtonCard(x + 14, cy, w - 28, "RNG Drop Position", "Drag the RNG announcement overlay.", "OPEN EDITOR",
                () -> Minecraft.getInstance().gui.setScreen(new RngDropPositionEditorScreen(this)));
    }

    private void buildFarming(int x, int y, int w) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        addHeader(x, y, "Farming RNG", "Track selected rare farming drops with persistent timers.");
        int cy = y + 68;
        addToggleCard(x + 14, cy, w - 28, "Farming RNG", "Master switch for farming RNG tracking.", () -> c.farmingRngEnabled,
                () -> toggle(() -> c.farmingRngEnabled = !c.farmingRngEnabled)); cy += 55;
        addToggleCard(x + 14, cy, w - 28, "Harvest Feast", "Track the configured Harvest Feast drops.", () -> c.harvestFeastEnabled,
                () -> toggle(() -> c.harvestFeastEnabled = !c.harvestFeastEnabled)); cy += 51;

        int colGap = 6, inner = w - 40, colW = (inner - colGap) / 2;
        for (int i = 0; i < HARVEST.length; i++) {
            String item = HARVEST[i];
            int col = i % 2, row = i / 2;
            addItemToggle(x + 14 + col * (colW + colGap), cy + row * 22, colW, item,
                    () -> c.harvestFeastDrops.getOrDefault(item, true),
                    () -> toggle(() -> c.harvestFeastDrops.put(item, !c.harvestFeastDrops.getOrDefault(item, true))));
        }
        cy += 9 * 22 + 7;
        addToggleCard(x + 14, cy, w - 28, "Slugs", "Track Epic and Legendary Slug drops.", () -> c.slugEnabled,
                () -> toggle(() -> c.slugEnabled = !c.slugEnabled)); cy += 51;
        addItemToggle(x + 14, cy, w - 28, "Epic Slug", () -> c.epicSlug,
                () -> toggle(() -> c.epicSlug = !c.epicSlug)); cy += 23;
        addItemToggle(x + 14, cy, w - 28, "Legendary Slug", () -> c.legendarySlug,
                () -> toggle(() -> c.legendarySlug = !c.legendarySlug));
    }

    private void buildPet(int x, int y, int w) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        addHeader(x, y, "Pet", "Configure the active pet display.");
        int cy = y + 68;
        addToggleCard(x + 14, cy, w - 28, "Pet Overlay", "Display the active pet information HUD.", () -> c.petOverlayEnabled,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled)); cy += 51;
        addToggleCard(x + 14, cy, w - 28, "Pet Icon", "Show the pet icon.", () -> c.showPetIcon,
                () -> toggle(() -> c.showPetIcon = !c.showPetIcon)); cy += 51;
        addToggleCard(x + 14, cy, w - 28, "Pet Level", "Show the current pet level.", () -> c.showPetLevel,
                () -> toggle(() -> c.showPetLevel = !c.showPetLevel)); cy += 51;
        addToggleCard(x + 14, cy, w - 28, "Pet Progress", "Show the XP progress bar.", () -> c.showPetProgress,
                () -> toggle(() -> c.showPetProgress = !c.showPetProgress)); cy += 51;
        addToggleCard(x + 14, cy, w - 28, "Pet XP", "Show total pet XP.", () -> c.showPetXp,
                () -> toggle(() -> c.showPetXp = !c.showPetXp)); cy += 51;
        addToggleCard(x + 14, cy, w - 28, "Overflow XP", "Show XP above the pet's maximum level.", () -> c.showOverflowXp,
                () -> toggle(() -> c.showOverflowXp = !c.showOverflowXp)); cy += 51;
        addToggleCard(x + 14, cy, w - 28, "Pet Item", "Show the held pet item.", () -> c.showPetItem,
                () -> toggle(() -> c.showPetItem = !c.showPetItem));
    }

    private void addHeader(int x, int y, String title, String subtitle) {
        addLabel(title, x + 14, y + 14, 0xFFE8E8EC);
        addLabel(subtitle, x + 14, y + 37, 0xFF999AA5);
    }

    private void addCard(int x, int y, int w, int h, String title, String desc) {
        addRenderableWidget(new PaintWidget(x, y, w, h, 2));
        addLabel(title, x + 12, y + 12, 0xFFE4E4EA);
        addLabel(desc, x + 12, y + 35, 0xFF9899A5);
    }

    private void addToggleCard(int x, int y, int w, String title, String desc, BooleanSupplier enabled, Runnable action) {
        addRenderableWidget(new PaintWidget(x, y, w, 46, 2));
        addLabel(title, x + 12, y + 8, 0xFFE3E3E9);
        addLabel(desc, x + 12, y + 27, 0xFF9697A3);
        addRenderableWidget(new ToggleButton(x + w - 68, y + 11, 55, 23, enabled, action));
    }

    private void addButtonCard(int x, int y, int w, String title, String desc, String button, Runnable action) {
        addRenderableWidget(new PaintWidget(x, y, w, 58, 2));
        addLabel(title, x + 12, y + 10, 0xFFE3E3E9);
        addLabel(desc, x + 12, y + 31, 0xFF9697A3);
        addRenderableWidget(new ActionButton(x + w - 120, y + 17, 107, 24, button, action));
    }

    private void addItemToggle(int x, int y, int w, String name, BooleanSupplier enabled, Runnable action) {
        addRenderableWidget(new PaintWidget(x, y, w, 20, 2));
        addLabel(name, x + 9, y + 5, 0xFFD8D8DF);
        addRenderableWidget(new ToggleButton(x + w - 43, y + 1, 38, 18, enabled, action));
    }

    private void addLabel(String text, int x, int y, int color) {
        addRenderableWidget(new LabelWidget(x, y, text, color));
    }

    private void toggle(Runnable action) { action.run(); SkyblockExtrasClient.CONFIG.save(); }

    private class LabelWidget extends AbstractWidget {
        private final int color;
        LabelWidget(int x, int y, String text, int color) { super(x, y, font.width(text) + 2, 10, Component.literal(text)); this.color = color; this.active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) { g.text(font, getMessage(), getX(), getY(), color, false); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class PaintWidget extends AbstractWidget {
        private final int kind;
        PaintWidget(int x, int y, int w, int h, int kind) { super(x, y, w, h, Component.empty()); this.kind = kind; this.active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            if (kind == 0) {
                g.fill(0, 0, width, height, 0x99080A0F);
                g.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFF090A0F);
                g.fill(left, top, left + panelWidth, top + panelHeight, 0xFF15161B);
                g.fill(left + 6, top + 6, left + panelWidth - 6, top + 47, 0xFF1B1C21);
                g.outline(left, top, panelWidth, panelHeight, 0xFF303139);
                g.horizontalLine(left + 6, left + panelWidth - 6, top + 48, 0xFF34353C);
                String title = "Skyblock Extras 0.1.2";
                g.text(font, title, left + 18, top + 20, 0xFFB8B8C2, false);
                g.text(font, "by SBE", left + 142, top + 20, 0xFFC276FF, false);
            } else if (kind == 1) {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF17181E);
                g.outline(getX(), getY(), width, height, 0xFF303139);
            } else {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF1C1D23);
                g.outline(getX(), getY(), width, height, 0xFF2B2C34);
            }
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class NavButton extends AbstractWidget {
        private final boolean selected; private final Runnable action;
        NavButton(int x, int y, int w, int h, String text, boolean selected, Runnable action) { super(x, y, w, h, Component.literal(text)); this.selected = selected; this.action = action; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isMouseOver(mx, my);
            g.fill(getX(), getY(), getX()+width, getY()+height, selected ? 0xFF6B3D8A : hover ? 0xFF292A32 : 0xFF202128);
            g.outline(getX(), getY(), width, height, selected ? 0xFFB96BFF : 0xFF34353D);
            int tw = font.width(getMessage());
            g.text(font, getMessage(), getX() + (width-tw)/2, getY()+10, selected ? 0xFFFFFFFF : 0xFFD5D5DC, false);
        }
        @Override public void onClick(MouseButtonEvent e, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class ActionButton extends AbstractWidget {
        private final Runnable action;
        ActionButton(int x, int y, int w, int h, String text, Runnable action) { super(x, y, w, h, Component.literal(text)); this.action = action; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isMouseOver(mx, my);
            g.fill(getX(), getY(), getX()+width, getY()+height, hover ? 0xFF7D48A0 : 0xFF5E386F);
            g.outline(getX(), getY(), width, height, 0xFFB96BFF);
            int tw = font.width(getMessage());
            g.text(font, getMessage(), getX()+(width-tw)/2, getY()+8, 0xFFFFFFFF, false);
        }
        @Override public void onClick(MouseButtonEvent e, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class ToggleButton extends AbstractWidget {
        private final BooleanSupplier enabled; private final Runnable action;
        ToggleButton(int x, int y, int w, int h, BooleanSupplier enabled, Runnable action) { super(x, y, w, h, Component.empty()); this.enabled = enabled; this.action = action; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean on = enabled.getAsBoolean(); boolean hover = isMouseOver(mx, my);
            g.fill(getX(), getY(), getX()+width, getY()+height, on ? 0xFF6E4690 : 0xFF34353D);
            if (hover) g.outline(getX(), getY(), width, height, 0xFFC276FF);
            String text = on ? "ON" : "OFF"; int tw = font.width(text);
            g.text(font, text, getX()+(width-tw)/2, getY()+(height-8)/2, on ? 0xFFFFFFFF : 0xFF9B9CA6, false);
        }
        @Override public void onClick(MouseButtonEvent e, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }
}
