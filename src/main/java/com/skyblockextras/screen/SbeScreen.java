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

        // Compact layout so the Farming RNG page fits on normal 720p/768p screens.
        widthBox = Math.min(1120, this.width - 20);
        heightBox = Math.min(650, this.height - 20);
        left = (this.width - widthBox) / 2;
        top = (this.height - heightBox) / 2;

        addRenderableWidget(new BackgroundWidget(left, top, widthBox, heightBox));
        addRenderableWidget(new SearchWidget(left + widthBox - 252, top + 12, 230, 30));

        // Compact sidebar.
        int sideX = left + 12;
        int sideY = top + 60;
        int sideW = 208;
        int sideH = heightBox - 96;
        addRenderableWidget(new PanelWidget(sideX, sideY, sideW, sideH, true));
        addText("CATEGORIES", sideX + 14, sideY + 12, 0xFFC276FF);

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String selected = CATEGORIES[i];
            int y = sideY + 36 + i * 37;
            addRenderableWidget(new CategoryButton(
                    sideX + 7, y, sideW - 14, 30,
                    CATEGORY_ICONS[i] + "  " + selected,
                    selected.equals(category),
                    () -> {
                        category = selected;
                        rebuildWidgets();
                    }
            ));
        }

        // Main content panel.
        int contentX = left + 232;
        int contentY = top + 60;
        int contentW = widthBox - 244;
        int contentH = heightBox - 96;
        addRenderableWidget(new PanelWidget(contentX, contentY, contentW, contentH, true));

        switch (category) {
            case "About" -> buildAbout(contentX, contentY, contentW);
            case "GUI" -> buildGui(contentX, contentY, contentW);
            case "Farming RNG" -> buildFarming(contentX, contentY, contentW);
            case "Pet" -> buildPet(contentX, contentY, contentW);
            default -> buildComingSoon(contentX, contentY, contentW);
        }

        addRenderableWidget(new ActionButton(
                left + widthBox - 104, top + heightBox - 34,
                88, 26, "Done", true, this::onClose
        ));
    }

    private void buildAbout(int x, int y, int w) {
        addHeader("ABOUT", "Skyblock Extras client-side utilities", x + 18, y + 18);
        addCard(x + 14, y + 62, w - 28, 84, "Skyblock Extras",
                "A client-side SkyBlock utility mod for Minecraft 26.2.");
        addText("VERSION 0.1.2", x + 28, y + 106, 0xFFC276FF);
        addText("Configure HUDs, RNG tracking and other utilities from this menu.",
                x + 28, y + 128, 0xFF9295A2);
    }

    private void buildGui(int x, int y, int w) {
        addHeader("GUI & HUD", "Configure overlays and the in-game position editor.", x + 18, y + 18);
        int innerX = x + 14;
        int innerW = w - 28;
        int yy = y + 62;

        addToggleCard("Pet Overlay", "Display the active pet information HUD.",
                () -> SkyblockExtrasClient.CONFIG.petOverlayEnabled,
                innerX, yy, innerW,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.petOverlayEnabled = !SkyblockExtrasClient.CONFIG.petOverlayEnabled));
        yy += 55;

        addCard(innerX, yy, innerW, 56, "POSITION / SIZE EDITOR",
                "Drag HUD elements and scroll to resize them.");
        addRenderableWidget(new ActionButton(innerX + innerW - 92, yy + 14, 76, 25,
                "OPEN", false, () -> Minecraft.getInstance().gui.setScreen(new SbeScreen(this))));
        yy += 68;

        addToggleCard("Farming RNG", "Show rare farming drops in chat with persistent timers.",
                () -> SkyblockExtrasClient.CONFIG.farmingRngEnabled,
                innerX, yy, innerW,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.farmingRngEnabled = !SkyblockExtrasClient.CONFIG.farmingRngEnabled));
    }

    private void buildFarming(int x, int y, int w) {
        addHeader("FARMING RNG", "Track rare farming drops in chat with persistent timers.", x + 18, y + 16);

        int innerX = x + 14;
        int innerW = w - 28;
        int yy = y + 58;

        // Master Farming RNG toggle.
        addToggleCard("Farming RNG", "Track rare farming drops.",
                () -> SkyblockExtrasClient.CONFIG.farmingRngEnabled,
                innerX, yy, innerW,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.farmingRngEnabled = !SkyblockExtrasClient.CONFIG.farmingRngEnabled));
        yy += 54;

        // Harvest Feast.
        yy = addSection("HARVEST FEAST", "Track Harvest Feast drops.",
                () -> SkyblockExtrasClient.CONFIG.harvestFeastEnabled,
                harvestExpanded,
                innerX, yy, innerW,
                () -> {
                    harvestExpanded = !harvestExpanded;
                    rebuildWidgets();
                },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.harvestFeastEnabled = !SkyblockExtrasClient.CONFIG.harvestFeastEnabled));

        if (harvestExpanded) {
            int gap = 5;
            int colW = (innerW - gap) / 2;
            int rowH = 23;

            for (int i = 0; i < HARVEST_FEAST_DROPS.length; i++) {
                String item = HARVEST_FEAST_DROPS[i];
                int col = i % 2;
                int row = i / 2;
                int ix = innerX + col * (colW + gap);
                int iy = yy + row * rowH;
                addItemToggle(item,
                        () -> SkyblockExtrasClient.CONFIG.harvestFeastDrops.getOrDefault(item, true),
                        ix, iy, colW,
                        () -> toggle(() -> SkyblockExtrasClient.CONFIG.harvestFeastDrops.put(
                                item,
                                !SkyblockExtrasClient.CONFIG.harvestFeastDrops.getOrDefault(item, true)
                        )));
            }

            // 17 items = 9 compact rows.
            yy += 9 * rowH + 6;
        }

        // Slugs.
        yy = addSection("SLUGS", "Track Epic and Legendary Slug drops.",
                () -> SkyblockExtrasClient.CONFIG.slugEnabled,
                slugsExpanded,
                innerX, yy, innerW,
                () -> {
                    slugsExpanded = !slugsExpanded;
                    rebuildWidgets();
                },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.slugEnabled = !SkyblockExtrasClient.CONFIG.slugEnabled));

        if (slugsExpanded) {
            addItemToggle("Epic Slug", () -> SkyblockExtrasClient.CONFIG.epicSlug,
                    innerX, yy, innerW,
                    () -> toggle(() -> SkyblockExtrasClient.CONFIG.epicSlug = !SkyblockExtrasClient.CONFIG.epicSlug));
            addItemToggle("Legendary Slug", () -> SkyblockExtrasClient.CONFIG.legendarySlug,
                    innerX, yy + 26, innerW,
                    () -> toggle(() -> SkyblockExtrasClient.CONFIG.legendarySlug = !SkyblockExtrasClient.CONFIG.legendarySlug));
            yy += 54;
        }

        // Farming dyes.
        addSection("FARMING DYES", "Track farming-related dye drops.",
                () -> SkyblockExtrasClient.CONFIG.dyesEnabled,
                dyesExpanded,
                innerX, yy, innerW,
                () -> {
                    dyesExpanded = !dyesExpanded;
                    rebuildWidgets();
                },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.dyesEnabled = !SkyblockExtrasClient.CONFIG.dyesEnabled));
    }

    private int addSection(String title, String description, BooleanSupplier enabled, boolean expanded,
                           int x, int y, int w, Runnable expand, Runnable toggleAction) {
        int h = expanded ? 34 : 44;

        addRenderableWidget(new SectionWidget(x, y, w, h));
        addRenderableWidget(new ActionButton(
                x + 6, y + 5, 218, 24,
                (expanded ? "⌄  " : "›  ") + title,
                false, expand
        ));
        addToggleButton("Enabled", enabled, x + w - 112, y + 6, 98, toggleAction);

        if (!expanded) {
            addText(description, x + 14, y + 30, 0xFF858895);
        }

        return y + h;
    }

    private void buildPet(int x, int y, int w) {
        addHeader("PET OVERLAY", "Choose which information is visible in the pet HUD.", x + 18, y + 16);
        int innerX = x + 14;
        int innerW = w - 28;
        int yy = y + 58;
        SbeConfig c = SkyblockExtrasClient.CONFIG;

        addToggleCard("Pet Overlay", "Display the pet HUD.", () -> c.petOverlayEnabled, innerX, yy, innerW,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled)); yy += 54;
        addToggleCard("Pet Icon", "Show the pet icon.", () -> c.showPetIcon, innerX, yy, innerW,
                () -> toggle(() -> c.showPetIcon = !c.showPetIcon)); yy += 54;
        addToggleCard("Pet Level", "Show the current pet level.", () -> c.showPetLevel, innerX, yy, innerW,
                () -> toggle(() -> c.showPetLevel = !c.showPetLevel)); yy += 54;
        addToggleCard("Pet Progress", "Show the XP progress bar.", () -> c.showPetProgress, innerX, yy, innerW,
                () -> toggle(() -> c.showPetProgress = !c.showPetProgress)); yy += 54;
        addToggleCard("Pet XP", "Show total pet XP.", () -> c.showPetXp, innerX, yy, innerW,
                () -> toggle(() -> c.showPetXp = !c.showPetXp)); yy += 54;
        addToggleCard("Overflow XP", "Show overflow XP.", () -> c.showOverflowXp, innerX, yy, innerW,
                () -> toggle(() -> c.showOverflowXp = !c.showOverflowXp)); yy += 54;
        addToggleCard("Pet Item", "Show the held pet item.", () -> c.showPetItem, innerX, yy, innerW,
                () -> toggle(() -> c.showPetItem = !c.showPetItem));
    }

    private void buildComingSoon(int x, int y, int w) {
        addHeader(category.toUpperCase(), "Settings for this category are coming next.", x + 18, y + 16);
        addCard(x + 14, y + 62, w - 28, 56, category,
                "This section is ready for the next SBE feature set.");
    }

    private void addHeader(String title, String subtitle, int x, int y) {
        addText(title, x, y, 0xFFF3F3F7);
        addText(subtitle, x, y + 21, 0xFF9295A2);
    }

    private void addCard(int x, int y, int w, int h, String title, String description) {
        addRenderableWidget(new CardWidget(x, y, w, h));
        addText(title, x + 12, y + 10, 0xFFE8E8EF);
        if (!description.isEmpty()) {
            addText(description, x + 12, y + 31, 0xFF9295A2);
        }
    }

    private void addToggleCard(String title, String description, BooleanSupplier enabled,
                               int x, int y, int w, Runnable action) {
        addCard(x, y, w, 48, title, description);
        addToggleButton("", enabled, x + w - 82, y + 12, 68, action);
    }

    private void addItemToggle(String name, BooleanSupplier enabled, int x, int y, int w, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 22));
        addText(name, x + 10, y + 6, 0xFFDADAE2);
        addToggleButton("", enabled, x + w - 61, y + 1, 50, action);
    }

    private void addToggleButton(String label, BooleanSupplier enabled, int x, int y, int w, Runnable action) {
        addRenderableWidget(new ToggleButton(x, y, w, 22, label, enabled, action));
    }

    private void addText(String text, int x, int y, int color) {
        addRenderableWidget(new LabelWidget(x, y, text, color));
    }

    private void toggle(Runnable action) {
        // Do not rebuild the screen here. The toggle widget reads the live setting
        // and animates toward the new state, so clicking feels smooth instead of
        // instantly destroying/recreating the widget.
        action.run();
        SkyblockExtrasClient.CONFIG.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private class BackgroundWidget extends AbstractWidget {
        BackgroundWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(0, 0, SbeScreen.this.width, SbeScreen.this.height, 0xFF07080D);
            g.fill(left - 1, top - 1, left + widthBox + 1, top + heightBox + 1, 0xFF0B0C12);
            g.outline(left - 1, top - 1, widthBox + 2, heightBox + 2, 0xFF8A42D2);

            g.fill(left, top, left + widthBox, top + 50, 0xFF0E1017);
            g.outline(left, top, widthBox, 50, 0xFF272A37);
            g.horizontalLine(left + 14, left + widthBox - 14, top + 49, 0xFF20232E);

            g.text(SbeScreen.this.font, "SKYBLOCK EXTRAS", left + 18, top + 17,
                    0xFFF4F4F7, false);
            g.text(SbeScreen.this.font, "0.1.2", left + 179, top + 17,
                    0xFFC276FF, false);
            g.text(SbeScreen.this.font, "— configuration", left + 217, top + 17,
                    0xFF858894, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class SearchWidget extends AbstractWidget {
        SearchWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.literal("Search..."));
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF11131B);
            g.outline(getX(), getY(), width, height, 0xFF303342);
            g.text(SbeScreen.this.font, "Search...", getX() + 11, getY() + 10,
                    0xFF777B89, false);
            g.text(SbeScreen.this.font, "⌕", getX() + width - 20, getY() + 8,
                    0xFFB5B7C1, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class PanelWidget extends AbstractWidget {
        private final boolean border;

        PanelWidget(int x, int y, int width, int height, boolean border) {
            super(x, y, width, height, Component.empty());
            this.border = border;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF0C0E14);
            if (border) g.outline(getX(), getY(), width, height, 0xFF252936);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class SectionWidget extends AbstractWidget {
        SectionWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF11131C);
            g.outline(getX(), getY(), width, height, 0xFF272A38);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class CardWidget extends AbstractWidget {
        CardWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF12141D);
            g.outline(getX(), getY(), width, height, 0xFF272A38);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class LabelWidget extends AbstractWidget {
        private final String text;
        private final int color;

        LabelWidget(int x, int y, String text, int color) {
            super(x, y, Math.max(1, SbeScreen.this.font.width(text) + 2), 18, Component.empty());
            this.text = text;
            this.color = color;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.text(SbeScreen.this.font, text, getX(), getY(), color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class CategoryButton extends AbstractWidget {
        private final String text;
        private final boolean selected;
        private final Runnable action;

        CategoryButton(int x, int y, int w, int h, String text, boolean selected, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.text = text;
            this.selected = selected;
            this.action = action;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int fill = selected ? 0xFF20182B : (hover ? 0xFF151824 : 0xFF10121A);
            int border = selected ? 0xFFC276FF : (hover ? 0xFF5C3B78 : 0xFF202330);

            g.fill(getX(), getY(), getX() + width, getY() + height, fill);
            g.outline(getX(), getY(), width, height, border);

            if (selected) {
                g.fill(getX(), getY(), getX() + 3, getY() + height, 0xFFC276FF);
            }

            g.text(SbeScreen.this.font, text, getX() + 11, getY() + 8,
                    selected ? 0xFFF0DFFF : 0xFFD9D9E1, false);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) {
            defaultButtonNarrationText(b);
        }
    }

    private class ActionButton extends AbstractWidget {
        private final Runnable action;
        private final boolean accent;

        ActionButton(int x, int y, int w, int h, String text, boolean accent, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.action = action;
            this.accent = accent;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int fill = accent ? 0xFF2A1A3B : (hover ? 0xFF242631 : 0xFF171922);
            int border = accent ? 0xFFC276FF : (hover ? 0xFF8A42D2 : 0xFF363947);

            g.fill(getX(), getY(), getX() + width, getY() + height, fill);
            g.outline(getX(), getY(), width, height, border);

            int tw = SbeScreen.this.font.width(getMessage());
            g.text(SbeScreen.this.font, getMessage(), getX() + (width - tw) / 2,
                    getY() + (height - 8) / 2,
                    accent ? 0xFFF2E6FF : 0xFFE0E0E7, false);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) {
            defaultButtonNarrationText(b);
        }
    }

    private class ToggleButton extends AbstractWidget {
        private final String label;
        private final BooleanSupplier enabledSupplier;
        private final Runnable action;
        private float progress;
        private long lastFrameNanos;

        ToggleButton(int x, int y, int w, int h, String label, BooleanSupplier enabledSupplier, Runnable action) {
            super(x, y, w, h, Component.literal(label));
            this.label = label;
            this.enabledSupplier = enabledSupplier;
            this.action = action;
            this.progress = enabledSupplier.getAsBoolean() ? 1.0f : 0.0f;
            this.lastFrameNanos = System.nanoTime();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean targetEnabled = enabledSupplier.getAsBoolean();
            float target = targetEnabled ? 1.0f : 0.0f;

            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000.0f);
            lastFrameNanos = now;

            // Smooth slide animation. 12 = fast, but still visibly animated.
            float amount = Math.min(1.0f, dt * 12.0f);
            progress += (target - progress) * amount;
            if (Math.abs(target - progress) < 0.002f) progress = target;

            boolean hover = isHovered();
            int offBg = 0xFF242630;
            int onBg = 0xFF9A4DE0;
            int bg = lerpColor(offBg, onBg, progress);

            int offOutline = 0xFF3B3D49;
            int onOutline = 0xFFB86AF0;
            int outline = lerpColor(offOutline, onOutline, progress);
            if (hover) outline = 0xFFE0B8FF;

            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.outline(getX(), getY(), width, height, outline);

            int travel = width - 19;
            int knob = getX() + 2 + Math.round(travel * progress);
            g.fill(knob, getY() + 3, knob + 15, getY() + height - 3, 0xFFF8F8FC);

            if (!label.isEmpty()) {
                String text = label + "  " + (targetEnabled ? "ON" : "OFF");
                int tw = SbeScreen.this.font.width(text);
                g.text(SbeScreen.this.font, text, getX() - tw - 8, getY() + 6,
                        0xFFBFC0CA, false);
            }
        }

        private int lerpColor(int from, int to, float amount) {
            amount = Math.max(0.0f, Math.min(1.0f, amount));
            int fa = (from >>> 24) & 0xFF;
            int fr = (from >>> 16) & 0xFF;
            int fg = (from >>> 8) & 0xFF;
            int fb = from & 0xFF;

            int ta = (to >>> 24) & 0xFF;
            int tr = (to >>> 16) & 0xFF;
            int tg = (to >>> 8) & 0xFF;
            int tb = to & 0xFF;

            int a = Math.round(fa + (ta - fa) * amount);
            int r = Math.round(fr + (tr - fr) * amount);
            int gr = Math.round(fg + (tg - fg) * amount);
            int b = Math.round(fb + (tb - fb) * amount);
            return (a << 24) | (r << 16) | (gr << 8) | b;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) {
            defaultButtonNarrationText(b);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
