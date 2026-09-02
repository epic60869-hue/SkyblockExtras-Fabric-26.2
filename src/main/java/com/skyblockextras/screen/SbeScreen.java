package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SbeScreen extends Screen {
    private final Screen parent;
    private String category = "Farming RNG";
    private int left, top, widthBox, heightBox;

    private boolean harvestExpanded = true;
    private boolean slugsExpanded = false;
    private boolean dyesExpanded = false;

    private static final String[] CATEGORIES = {
            "About", "GUI", "Farming RNG", "Inventory", "Chat", "Bazaar", "Hunting", "Pet", "Misc"
    };

    private static final String[] CATEGORY_ICONS = {
            "i", "□", "✿", "▣", "○", "$", "×", "♣", "⚙"
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

        widthBox = Math.min(1260, this.width - 36);
        heightBox = Math.min(790, this.height - 28);
        left = (this.width - widthBox) / 2;
        top = (this.height - heightBox) / 2;

        addRenderableWidget(new BackgroundWidget(left, top, widthBox, heightBox));

        // Header search box: decorative for now, ready for real filtering later.
        addRenderableWidget(new SearchWidget(left + widthBox - 300, top + 16, 276, 38));

        // Sidebar.
        int sideX = left + 18;
        int sideY = top + 76;
        int sideW = 252;
        int sideH = heightBox - 130;
        addRenderableWidget(new PanelWidget(sideX, sideY, sideW, sideH, true));
        addText("CATEGORIES", sideX + 20, sideY + 18, 0xFFC276FF);

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String selected = CATEGORIES[i];
            int y = sideY + 50 + i * 52;
            addRenderableWidget(new CategoryButton(sideX + 12, y, sideW - 24, 42,
                    CATEGORY_ICONS[i] + "  " + selected, selected.equals(category), () -> {
                        category = selected;
                        rebuildWidgets();
                    }));
        }

        // Main panel.
        int contentX = left + 284;
        int contentY = top + 76;
        int contentW = widthBox - 302;
        int contentH = heightBox - 130;
        addRenderableWidget(new PanelWidget(contentX, contentY, contentW, contentH, true));

        switch (category) {
            case "About" -> buildAbout(contentX, contentY, contentW);
            case "GUI" -> buildGui(contentX, contentY, contentW);
            case "Farming RNG" -> buildFarming(contentX, contentY, contentW);
            case "Pet" -> buildPet(contentX, contentY, contentW);
            default -> buildComingSoon(contentX, contentY, contentW);
        }

        addRenderableWidget(new ActionButton(left + widthBox - 138, top + heightBox - 42,
                120, 30, "Done", false, this::onClose));
    }

    private void buildAbout(int x, int y, int w) {
        addHeader("ABOUT", "Skyblock Extras client-side utilities", x + 24, y + 24);
        addCard(x + 20, y + 82, w - 40, 106, "Skyblock Extras",
                "A client-side SkyBlock utility mod for Minecraft 26.2.");
        addText("VERSION 0.1.2", x + 36, y + 132, 0xFFC276FF);
        addText("Configure HUDs, RNG tracking and other utilities from this menu.",
                x + 36, y + 157, 0xFF9295A2);
    }

    private void buildGui(int x, int y, int w) {
        addHeader("GUI & HUD", "Configure overlays and the in-game position editor.", x + 24, y + 24);
        int innerX = x + 20;
        int innerW = w - 40;
        int yy = y + 82;

        addToggleCard("Pet Overlay", "Display the active pet information HUD.",
                SkyblockExtrasClient.CONFIG.petOverlayEnabled, innerX, yy, innerW,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.petOverlayEnabled = !SkyblockExtrasClient.CONFIG.petOverlayEnabled));
        yy += 68;

        addCard(innerX, yy, innerW, 72, "POSITION / SIZE EDITOR",
                "Drag HUD elements and scroll to resize them.");
        addRenderableWidget(new ActionButton(innerX + innerW - 112, yy + 22, 92, 28,
                "OPEN", false, () -> Minecraft.getInstance().gui.setScreen(new SbeScreen(this))));
        yy += 88;

        addToggleCard("Farming RNG", "Show rare farming drops in chat with persistent timers.",
                SkyblockExtrasClient.CONFIG.farmingRngEnabled, innerX, yy, innerW,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.farmingRngEnabled = !SkyblockExtrasClient.CONFIG.farmingRngEnabled));
    }

    private void buildFarming(int x, int y, int w) {
        addHeader("FARMING RNG", "Track rare farming drops in chat with persistent timers.", x + 24, y + 24);

        int innerX = x + 20;
        int innerW = w - 40;
        int yy = y + 78;

        addToggleCard("Farming RNG", "Track rare farming drops.",
                SkyblockExtrasClient.CONFIG.farmingRngEnabled, innerX, yy, innerW,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.farmingRngEnabled = !SkyblockExtrasClient.CONFIG.farmingRngEnabled));
        yy += 66;

        yy = addSection("HARVEST FEAST", "Track Harvest Feast drops.",
                SkyblockExtrasClient.CONFIG.harvestFeastEnabled, harvestExpanded,
                innerX, yy, innerW,
                () -> { harvestExpanded = !harvestExpanded; rebuildWidgets(); },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.harvestFeastEnabled = !SkyblockExtrasClient.CONFIG.harvestFeastEnabled));

        if (harvestExpanded) {
            int gap = 8;
            int colW = (innerW - gap) / 2;
            int rowH = 30;
            for (int i = 0; i < HARVEST_FEAST_DROPS.length; i++) {
                String item = HARVEST_FEAST_DROPS[i];
                int col = i % 2;
                int row = i / 2;
                int ix = innerX + col * (colW + gap);
                int iy = yy + row * rowH;
                boolean enabled = SkyblockExtrasClient.CONFIG.harvestFeastDrops.getOrDefault(item, true);
                addItemToggle(item, enabled, ix, iy, colW,
                        () -> toggle(() -> SkyblockExtrasClient.CONFIG.harvestFeastDrops.put(item, !SkyblockExtrasClient.CONFIG.harvestFeastDrops.getOrDefault(item, true))));
            }
            yy += 9 * rowH + 12;
        }

        yy = addSection("SLUGS", "Track Epic and Legendary Slug drops.",
                SkyblockExtrasClient.CONFIG.slugEnabled, slugsExpanded,
                innerX, yy, innerW,
                () -> { slugsExpanded = !slugsExpanded; rebuildWidgets(); },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.slugEnabled = !SkyblockExtrasClient.CONFIG.slugEnabled));

        if (slugsExpanded) {
            addItemToggle("Epic Slug", SkyblockExtrasClient.CONFIG.epicSlug, innerX, yy, innerW,
                    () -> toggle(() -> SkyblockExtrasClient.CONFIG.epicSlug = !SkyblockExtrasClient.CONFIG.epicSlug));
            addItemToggle("Legendary Slug", SkyblockExtrasClient.CONFIG.legendarySlug, innerX, yy + 34, innerW,
                    () -> toggle(() -> SkyblockExtrasClient.CONFIG.legendarySlug = !SkyblockExtrasClient.CONFIG.legendarySlug));
            yy += 72;
        }

        addSection("FARMING DYES", "Track farming-related dye drops.",
                SkyblockExtrasClient.CONFIG.dyesEnabled, dyesExpanded,
                innerX, yy, innerW,
                () -> { dyesExpanded = !dyesExpanded; rebuildWidgets(); },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.dyesEnabled = !SkyblockExtrasClient.CONFIG.dyesEnabled));
    }

    private int addSection(String title, String description, boolean enabled, boolean expanded,
                           int x, int y, int w, Runnable expand, Runnable toggleAction) {
        int h = expanded ? 42 : 56;
        addRenderableWidget(new SectionWidget(x, y, w, h, title, description, expanded));
        addRenderableWidget(new ActionButton(x + 8, y + 8, 245, 28,
                (expanded ? "⌄  " : "›  ") + title, false, expand));
        addToggleButton("Enabled", enabled, x + w - 126, y + 9, 108, toggleAction);
        if (!expanded) addText(description, x + 18, y + 37, 0xFF858895);
        return y + h;
    }

    private void buildPet(int x, int y, int w) {
        addHeader("PET OVERLAY", "Choose which information is visible in the pet HUD.", x + 24, y + 24);
        int innerX = x + 20;
        int innerW = w - 40;
        int yy = y + 78;
        SbeConfig c = SkyblockExtrasClient.CONFIG;

        addToggleCard("Pet Overlay", "Display the pet HUD.", c.petOverlayEnabled, innerX, yy, innerW,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled)); yy += 64;
        addToggleCard("Pet Icon", "Show the pet icon.", c.showPetIcon, innerX, yy, innerW,
                () -> toggle(() -> c.showPetIcon = !c.showPetIcon)); yy += 64;
        addToggleCard("Pet Level", "Show the current pet level.", c.showPetLevel, innerX, yy, innerW,
                () -> toggle(() -> c.showPetLevel = !c.showPetLevel)); yy += 64;
        addToggleCard("Pet Progress", "Show the XP progress bar.", c.showPetProgress, innerX, yy, innerW,
                () -> toggle(() -> c.showPetProgress = !c.showPetProgress)); yy += 64;
        addToggleCard("Pet XP", "Show total pet XP.", c.showPetXp, innerX, yy, innerW,
                () -> toggle(() -> c.showPetXp = !c.showPetXp)); yy += 64;
        addToggleCard("Overflow XP", "Show overflow XP.", c.showOverflowXp, innerX, yy, innerW,
                () -> toggle(() -> c.showOverflowXp = !c.showOverflowXp)); yy += 64;
        addToggleCard("Pet Item", "Show the held pet item.", c.showPetItem, innerX, yy, innerW,
                () -> toggle(() -> c.showPetItem = !c.showPetItem));
    }

    private void buildComingSoon(int x, int y, int w) {
        addHeader(category.toUpperCase(), "Settings for this category are coming next.", x + 24, y + 24);
        addCard(x + 20, y + 82, w - 40, 72, category,
                "This section is ready for the next SBE feature set.");
    }

    private void addHeader(String title, String subtitle, int x, int y) {
        addText(title, x, y, 0xFFF3F3F7);
        addText(subtitle, x, y + 25, 0xFF9295A2);
    }

    private void addCard(int x, int y, int w, int h, String title, String description) {
        addRenderableWidget(new CardWidget(x, y, w, h));
        addText(title, x + 14, y + 13, 0xFFE8E8EF);
        if (!description.isEmpty()) addText(description, x + 14, y + 36, 0xFF9295A2);
    }

    private void addToggleCard(String title, String description, boolean enabled,
                               int x, int y, int w, Runnable action) {
        addCard(x, y, w, 54, title, description);
        addToggleButton("", enabled, x + w - 94, y + 15, 76, action);
    }

    private void addItemToggle(String name, boolean enabled, int x, int y, int w, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 28));
        addText(name, x + 12, y + 8, 0xFFDADAE2);
        addToggleButton("", enabled, x + w - 72, y + 3, 60, action);
    }

    private void addToggleButton(String label, boolean enabled, int x, int y, int w, Runnable action) {
        addRenderableWidget(new ToggleButton(x, y, w, 24, label, enabled, action));
    }

    private void addText(String text, int x, int y, int color) {
        addRenderableWidget(new LabelWidget(x, y, text, color));
    }

    private void toggle(Runnable action) {
        action.run();
        SkyblockExtrasClient.CONFIG.save();
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private class BackgroundWidget extends AbstractWidget {
        BackgroundWidget(int x, int y, int width, int height) { super(x, y, width, height, Component.empty()); }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(0, 0, SbeScreen.this.width, SbeScreen.this.height, 0xFF07080D);
            g.fill(left - 1, top - 1, left + widthBox + 1, top + heightBox + 1, 0xFF0B0C12);
            g.outline(left - 1, top - 1, widthBox + 2, heightBox + 2, 0xFF8A42D2);
            g.fill(left, top, left + widthBox, top + 62, 0xFF0E1017);
            g.outline(left, top, widthBox, 62, 0xFF272A37);
            g.horizontalLine(left + 20, left + widthBox - 20, top + 61, 0xFF20232E);
            g.text(SbeScreen.this.font, "SKYBLOCK EXTRAS", left + 24, top + 21, 0xFFF4F4F7, false);
            g.text(SbeScreen.this.font, "0.1.2", left + 183, top + 21, 0xFFC276FF, false);
            g.text(SbeScreen.this.font, "— configuration", left + 219, top + 21, 0xFF858894, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { }
        @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    }

    private class SearchWidget extends AbstractWidget {
        SearchWidget(int x, int y, int width, int height) { super(x, y, width, height, Component.literal("Search...")); }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF11131B);
            g.outline(getX(), getY(), width, height, isHovered() ? 0xFF8A42D2 : 0xFF303342);
            g.text(SbeScreen.this.font, "Search...", getX() + 14, getY() + 14, 0xFF777B89, false);
            g.text(SbeScreen.this.font, "⌕", getX() + width - 25, getY() + 12, 0xFFB5B7C1, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { }
        @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    }

    private class PanelWidget extends AbstractWidget {
        private final boolean border;
        PanelWidget(int x, int y, int width, int height, boolean border) { super(x, y, width, height, Component.empty()); this.border = border; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF0C0E14);
            if (border) g.outline(getX(), getY(), width, height, 0xFF252936);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { }
        @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    }

    private class SectionWidget extends AbstractWidget {
        private final String title, description;
        private final boolean expanded;
        SectionWidget(int x, int y, int w, int h, String title, String description, boolean expanded) {
            super(x, y, w, h, Component.empty()); this.title = title; this.description = description; this.expanded = expanded;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF11131C);
            g.outline(getX(), getY(), width, height, 0xFF272A38);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { }
        @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    }

    private class CardWidget extends AbstractWidget {
        CardWidget(int x, int y, int w, int h) { super(x, y, w, h, Component.empty()); }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF12141D);
            g.outline(getX(), getY(), width, height, 0xFF272A38);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { }
        @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    }

    private class LabelWidget extends AbstractWidget {
        private final String text; private final int color;
        LabelWidget(int x, int y, String text, int color) { super(x, y, Math.max(1, SbeScreen.this.font.width(text) + 2), 18, Component.empty()); this.text = text; this.color = color; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) { g.text(SbeScreen.this.font, text, getX(), getY(), color, false); }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { }
        @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    }

    private class CategoryButton extends AbstractWidget {
        private final String text; private final boolean selected; private final Runnable action;
        CategoryButton(int x, int y, int w, int h, String text, boolean selected, Runnable action) {
            super(x, y, w, h, Component.literal(text)); this.text = text; this.selected = selected; this.action = action;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int fill = selected ? 0xFF20182B : (hover ? 0xFF151824 : 0xFF10121A);
            int border = selected ? 0xFFC276FF : (hover ? 0xFF5C3B78 : 0xFF202330);
            g.fill(getX(), getY(), getX() + width, getY() + height, fill);
            g.outline(getX(), getY(), width, height, border);
            if (selected) g.fill(getX(), getY(), getX() + 4, getY() + height, 0xFFC276FF);
            g.text(SbeScreen.this.font, text, getX() + 14, getY() + 13, selected ? 0xFFF0DFFF : 0xFFD9D9E1, false);
        }
        @Override public void onClick(double x, double y) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { defaultButtonNarrationText(b); }
    }

    private class ActionButton extends AbstractWidget {
        private final Runnable action; private final boolean accent;
        ActionButton(int x, int y, int w, int h, String text, boolean accent, Runnable action) {
            super(x, y, w, h, Component.literal(text)); this.action = action; this.accent = accent;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int fill = accent ? 0xFF2A1A3B : (hover ? 0xFF242631 : 0xFF171922);
            int border = accent ? 0xFFC276FF : (hover ? 0xFF8A42D2 : 0xFF363947);
            g.fill(getX(), getY(), getX() + width, getY() + height, fill);
            g.outline(getX(), getY(), width, height, border);
            int tw = SbeScreen.this.font.width(getMessage());
            g.text(SbeScreen.this.font, getMessage(), getX() + (width - tw) / 2, getY() + (height - 8) / 2,
                    accent ? 0xFFF2E6FF : 0xFFE0E0E7, false);
        }
        @Override public void onClick(double x, double y) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { defaultButtonNarrationText(b); }
    }

    private class ToggleButton extends AbstractWidget {
        private final String label; private final boolean enabled; private final Runnable action;
        ToggleButton(int x, int y, int w, int h, String label, boolean enabled, Runnable action) {
            super(x, y, w, h, Component.literal(label)); this.label = label; this.enabled = enabled; this.action = action;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int bg = enabled ? 0xFF9A4DE0 : 0xFF242630;
            int outline = hover ? 0xFFE0B8FF : (enabled ? 0xFFB86AF0 : 0xFF3B3D49);
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.outline(getX(), getY(), width, height, outline);
            int knob = enabled ? getX() + width - 19 : getX() + 3;
            g.fill(knob, getY() + 4, knob + 16, getY() + height - 4, 0xFFF8F8FC);
            if (!label.isEmpty()) {
                String text = label + "  " + (enabled ? "ON" : "OFF");
                int tw = SbeScreen.this.font.width(text);
                g.text(SbeScreen.this.font, text, getX() - tw - 10, getY() + 7, 0xFFBFC0CA, false);
            }
        }
        @Override public void onClick(double x, double y) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { defaultButtonNarrationText(b); }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
