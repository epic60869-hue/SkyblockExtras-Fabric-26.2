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

/**
 * Compact classic SkyBlock-style configuration screen.
 */
public class SbeScreen extends Screen {
    private final Screen parent;

    // /sbe always starts on About. The user can then select Farming RNG from the sidebar.
    private String category = "About";
    private int left, top, widthBox, heightBox;
    private int contentX, contentY, contentW, contentH;

    private boolean harvestExpanded = true;
    private boolean slugsExpanded = false;
    private boolean dyesExpanded = false;

    private static final String[] CATEGORIES = {
            "About", "GUI", "Farming RNG", "Pet"
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
        clearWidgets();

        // Compact dimensions so the complete settings window fits on normal screens.
        widthBox = Math.min(930, width - 24);
        heightBox = Math.min(700, height - 24);
        left = (width - widthBox) / 2;
        top = (height - heightBox) / 2;

        addRenderableWidget(new BackgroundWidget(left, top, widthBox, heightBox));
        addRenderableWidget(new SearchIconWidget(left + widthBox - 45, top + 12, 28, 28));

        // Sidebar: only the four currently implemented categories.
        int sideX = left + 8;
        int sideY = top + 54;
        int sideW = 246;
        int sideH = heightBox - 62;
        addRenderableWidget(new PanelWidget(sideX, sideY, sideW, sideH));
        addText("Categories", sideX + (sideW - font.width("Categories")) / 2,
                sideY + 17, 0xFFB86AF0);

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String selected = CATEGORIES[i];
            int y = sideY + 48 + i * 39;
            addRenderableWidget(new CategoryButton(
                    sideX + 16, y, sideW - 32, 31,
                    selected, selected.equals(category),
                    () -> {
                        category = selected;
                        rebuildWidgets();
                    }
            ));
        }

        contentX = left + 252;
        contentY = top + 54;
        contentW = widthBox - 260;
        contentH = heightBox - 62;
        addRenderableWidget(new PanelWidget(contentX, contentY, contentW, contentH));

        switch (category) {
            case "About" -> buildAbout();
            case "GUI" -> buildGui();
            case "Farming RNG" -> buildFarming();
            case "Pet" -> buildPet();
            default -> buildAbout();
        }

        addRenderableWidget(new ActionButton(
                left + widthBox - 102, top + heightBox - 36,
                86, 27, "Done", true, this::onClose
        ));
    }

    private int innerLeft() {
        return contentX + 16;
    }

    private int innerWidth() {
        return contentW - 32;
    }

    private void buildAbout() {
        addHeader("About", "Skyblock Extras client-side utilities.");
        addCard(innerLeft(), contentY + 67, innerWidth(), 82,
                "Skyblock Extras 0.1.2",
                "A client-side SkyBlock utility mod for Minecraft 26.2.");
        addText("Configure HUDs, farming RNG and other utilities from this menu.",
                innerLeft() + 12, contentY + 125, 0xFF9B9CA8);
    }

    private void buildGui() {
        addHeader("GUI and HUD", "Configure overlays and the in-game position editor.");
        int x = innerLeft();
        int w = innerWidth();
        int y = contentY + 62;

        addToggleCard("Pet Overlay", "Display the active pet information HUD.",
                () -> SkyblockExtrasClient.CONFIG.petOverlayEnabled, x, y, w,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.petOverlayEnabled =
                        !SkyblockExtrasClient.CONFIG.petOverlayEnabled));
        y += 51;

        addCard(x, y, w, 55, "Position Editor (Keybind)",
                "Press the configured key to open the Position Editor.");
        addText("Position Editor", x + 11, y + 12, 0xFFE5E5EA);
        addRenderableWidget(new ActionButton(x + 12, y + 31, 92, 19,
                "OPEN EDITOR", false,
                () -> Minecraft.getInstance().gui.setScreen(new SbeScreen(this))));
        y += 63;

        addToggleCard("Farming RNG", "Show rare farming drops in chat with persistent timers.",
                () -> SkyblockExtrasClient.CONFIG.farmingRngEnabled, x, y, w,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.farmingRngEnabled =
                        !SkyblockExtrasClient.CONFIG.farmingRngEnabled));
    }

    private void buildFarming() {
        addHeader("Farming RNG", "Track rare farming drops in chat with persistent timers.");

        int x = innerLeft();
        int w = innerWidth();
        int y = contentY + 60;

        addToggleCard("Farming RNG", "Track rare farming drops.",
                () -> SkyblockExtrasClient.CONFIG.farmingRngEnabled, x, y, w,
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.farmingRngEnabled =
                        !SkyblockExtrasClient.CONFIG.farmingRngEnabled));
        y += 52;

        y = addSection("Harvest Feast", "Track Harvest Feast drops.",
                () -> SkyblockExtrasClient.CONFIG.harvestFeastEnabled,
                harvestExpanded, x, y, w,
                () -> {
                    harvestExpanded = !harvestExpanded;
                    rebuildWidgets();
                },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.harvestFeastEnabled =
                        !SkyblockExtrasClient.CONFIG.harvestFeastEnabled));

        if (harvestExpanded) {
            int gap = 5;
            int colW = (w - gap) / 2;
            int rowH = 21;

            for (int i = 0; i < HARVEST_FEAST_DROPS.length; i++) {
                String item = HARVEST_FEAST_DROPS[i];
                int col = i % 2;
                int row = i / 2;
                int ix = x + col * (colW + gap);
                int iy = y + row * rowH;

                addItemToggle(item,
                        () -> SkyblockExtrasClient.CONFIG.harvestFeastDrops
                                .getOrDefault(item, true),
                        ix, iy, colW,
                        () -> toggle(() -> SkyblockExtrasClient.CONFIG.harvestFeastDrops.put(
                                item,
                                !SkyblockExtrasClient.CONFIG.harvestFeastDrops
                                        .getOrDefault(item, true)
                        )));
            }

            y += 9 * rowH + 5;
        }

        y = addSection("Slugs", "Track Epic and Legendary Slug drops.",
                () -> SkyblockExtrasClient.CONFIG.slugEnabled,
                slugsExpanded, x, y, w,
                () -> {
                    slugsExpanded = !slugsExpanded;
                    rebuildWidgets();
                },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.slugEnabled =
                        !SkyblockExtrasClient.CONFIG.slugEnabled));

        if (slugsExpanded) {
            addItemToggle("Epic Slug", () -> SkyblockExtrasClient.CONFIG.epicSlug,
                    x, y, w,
                    () -> toggle(() -> SkyblockExtrasClient.CONFIG.epicSlug =
                            !SkyblockExtrasClient.CONFIG.epicSlug));
            addItemToggle("Legendary Slug", () -> SkyblockExtrasClient.CONFIG.legendarySlug,
                    x, y + 23, w,
                    () -> toggle(() -> SkyblockExtrasClient.CONFIG.legendarySlug =
                            !SkyblockExtrasClient.CONFIG.legendarySlug));
            y += 48;
        }

        addSection("Farming Dyes", "Track farming-related dye drops.",
                () -> SkyblockExtrasClient.CONFIG.dyesEnabled,
                dyesExpanded, x, y, w,
                () -> {
                    dyesExpanded = !dyesExpanded;
                    rebuildWidgets();
                },
                () -> toggle(() -> SkyblockExtrasClient.CONFIG.dyesEnabled =
                        !SkyblockExtrasClient.CONFIG.dyesEnabled));
    }

    private int addSection(String title, String description, BooleanSupplier enabled,
                           boolean expanded, int x, int y, int w,
                           Runnable expand, Runnable toggleAction) {
        int h = expanded ? 30 : 48;

        addRenderableWidget(new SectionWidget(x, y, w, h));
        addRenderableWidget(new ActionButton(
                x + 6, y + 4, 210, 22,
                (expanded ? "▼  " : "▶  ") + title,
                false, expand
        ));
        addToggleButton("Enabled", enabled, x + w - 105, y + 5, 91, toggleAction);

        if (!expanded) {
            addText(description, x + 12, y + 31, 0xFF898A96);
        }

        return y + h;
    }

    private void buildPet() {
        addHeader("Pet Overlay", "Choose which information is visible in the pet HUD.");
        int x = innerLeft();
        int w = innerWidth();
        int y = contentY + 60;
        SbeConfig c = SkyblockExtrasClient.CONFIG;

        addToggleCard("Pet Overlay", "Display the pet HUD.", () -> c.petOverlayEnabled, x, y, w,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled)); y += 51;
        addToggleCard("Pet Icon", "Show the pet icon.", () -> c.showPetIcon, x, y, w,
                () -> toggle(() -> c.showPetIcon = !c.showPetIcon)); y += 51;
        addToggleCard("Pet Level", "Show the current pet level.", () -> c.showPetLevel, x, y, w,
                () -> toggle(() -> c.showPetLevel = !c.showPetLevel)); y += 51;
        addToggleCard("Pet Progress", "Show the XP progress bar.", () -> c.showPetProgress, x, y, w,
                () -> toggle(() -> c.showPetProgress = !c.showPetProgress)); y += 51;
        addToggleCard("Pet XP", "Show total pet XP.", () -> c.showPetXp, x, y, w,
                () -> toggle(() -> c.showPetXp = !c.showPetXp)); y += 51;
        addToggleCard("Overflow XP", "Show overflow XP.", () -> c.showOverflowXp, x, y, w,
                () -> toggle(() -> c.showOverflowXp = !c.showOverflowXp)); y += 51;
        addToggleCard("Pet Item", "Show the held pet item.", () -> c.showPetItem, x, y, w,
                () -> toggle(() -> c.showPetItem = !c.showPetItem));
    }

    private void addHeader(String title, String subtitle) {
        int x = innerLeft();
        addText(title, x, contentY + 15, 0xFFE8E8EC);
        addText(subtitle, x, contentY + 38, 0xFF9B9CA8);
    }

    private void addCard(int x, int y, int w, int h, String title, String description) {
        addRenderableWidget(new CardWidget(x, y, w, h));
        addText(title, x + 11, y + 10, 0xFFE2E2E8);
        if (!description.isEmpty()) {
            addText(description, x + 11, y + 30, 0xFF9899A5);
        }
    }

    private void addToggleCard(String title, String description, BooleanSupplier enabled,
                               int x, int y, int w, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 46));
        addText(title, x + 11, y + 9, 0xFFE3E3E9);
        addText(description, x + 11, y + 28, 0xFF9697A3);
        addToggleButton("", enabled, x + w - 72, y + 11, 58, action);
    }

    private void addItemToggle(String name, BooleanSupplier enabled,
                               int x, int y, int w, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 20));
        addText(name, x + 9, y + 5, 0xFFD7D7DE);
        addToggleButton("", enabled, x + w - 51, y + 1, 40, action);
    }

    private void addToggleButton(String label, BooleanSupplier enabled,
                                 int x, int y, int w, Runnable action) {
        addRenderableWidget(new ToggleButton(x, y, w, 20, label, enabled, action));
    }

    private void addText(String text, int x, int y, int color) {
        addRenderableWidget(new LabelWidget(x, y, text, color));
    }

    private void toggle(Runnable action) {
        action.run();
        SkyblockExtrasClient.CONFIG.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private class BackgroundWidget extends AbstractWidget {
        BackgroundWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(0, 0, SbeScreen.this.width, SbeScreen.this.height, 0x990A0B10);
            g.fill(left - 3, top - 3, left + widthBox + 3, top + heightBox + 3, 0xFF090A0F);
            g.fill(left - 1, top - 1, left + widthBox + 1, top + heightBox + 1, 0xFF24252B);
            g.fill(left, top, left + widthBox, top + heightBox, 0xFF15161B);
            g.outline(left, top, widthBox, heightBox, 0xFF303139);

            g.fill(left + 5, top + 5, left + widthBox - 5, top + 45, 0xFF1B1C21);
            g.outline(left + 5, top + 5, widthBox - 10, 40, 0xFF0B0C10);
            g.horizontalLine(left + 5, left + widthBox - 5, top + 46, 0xFF34353C);

            String title = "Skyblock Extras 0.1.2";
            int tw = SbeScreen.this.font.width(title);
            g.text(SbeScreen.this.font, title,
                    left + (widthBox - tw) / 2, top + 17, 0xFFB8B8C2, false);
            g.text(SbeScreen.this.font, "by SBE",
                    left + (widthBox + tw) / 2 + 7, top + 17, 0xFFC276FF, false);
            g.horizontalLine(left + 5, left + widthBox - 5,
                    top + heightBox - 8, 0xFF34353C);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class PanelWidget extends AbstractWidget {
        PanelWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF17181E);
            g.outline(getX(), getY(), width, height, 0xFF303139);
            g.outline(getX() + 2, getY() + 2, width - 4, height - 4, 0xFF101116);
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
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF1E1F25);
            g.outline(getX(), getY(), width, height, 0xFF0D0E12);
            g.outline(getX() + 1, getY() + 1, width - 2, height - 2, 0xFF2E2F36);
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
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF202126);
            g.outline(getX(), getY(), width, height, 0xFF111217);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class LabelWidget extends AbstractWidget {
        private final String text;
        private final int color;

        LabelWidget(int x, int y, String text, int color) {
            super(x, y, Math.max(1, SbeScreen.this.font.width(text) + 2), 16, Component.empty());
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

    private class SearchIconWidget extends AbstractWidget {
        SearchIconWidget(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.text(SbeScreen.this.font, "⌕", getX(), getY(), 0xFFE0E0E6, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput b) { }
    }

    private class CategoryButton extends AbstractWidget {
        private final String text;
        private final boolean selected;
        private final Runnable action;

        CategoryButton(int x, int y, int w, int h, String text,
                       boolean selected, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.text = text;
            this.selected = selected;
            this.action = action;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int fill = selected ? 0xFF24252B : (hover ? 0xFF202127 : 0xFF191A20);
            int border = selected ? 0xFFB56AE9 : (hover ? 0xFF777783 : 0xFF303139);

            g.fill(getX(), getY(), getX() + width, getY() + height, fill);
            g.outline(getX(), getY(), width, height, border);
            if (selected) {
                g.fill(getX(), getY(), getX() + 3, getY() + height, 0xFFC276FF);
            }

            int tw = SbeScreen.this.font.width(text);
            g.text(SbeScreen.this.font, text,
                    getX() + (width - tw) / 2, getY() + 10,
                    selected ? 0xFFE9D6F7 : 0xFFC9C9D2, false);
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

        ActionButton(int x, int y, int w, int h, String text,
                     boolean accent, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.action = action;
            this.accent = accent;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            int fill = accent ? 0xFF33223F : (hover ? 0xFF292A31 : 0xFF222329);
            int border = accent ? 0xFFB86AF0 : (hover ? 0xFF8E8F99 : 0xFF4A4B54);

            g.fill(getX(), getY(), getX() + width, getY() + height, fill);
            g.outline(getX(), getY(), width, height, border);

            int tw = SbeScreen.this.font.width(getMessage());
            g.text(SbeScreen.this.font, getMessage(),
                    getX() + (width - tw) / 2,
                    getY() + (height - 8) / 2,
                    accent ? 0xFFEBDCF4 : 0xFFD9D9E0, false);
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

        ToggleButton(int x, int y, int w, int h, String label,
                     BooleanSupplier enabledSupplier, Runnable action) {
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
            float dt = Math.min(0.05f,
                    (now - lastFrameNanos) / 1_000_000_000.0f);
            lastFrameNanos = now;

            float amount = Math.min(1.0f, dt * 12.0f);
            progress += (target - progress) * amount;
            if (Math.abs(target - progress) < 0.002f) {
                progress = target;
            }

            boolean hover = isHovered();
            int bg = lerpColor(0xFF24252B, 0xFF8F4FD0, progress);
            int outline = lerpColor(0xFF4A4B54, 0xFFB86AF0, progress);
            if (hover) outline = 0xFFE3BFFF;

            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.outline(getX(), getY(), width, height, outline);

            int travel = width - 17;
            int knob = getX() + 1 + Math.round(travel * progress);
            g.fill(knob, getY() + 3, knob + 14,
                    getY() + height - 3, 0xFFF2F2F5);

            if (!label.isEmpty()) {
                String text = label + "  " + (targetEnabled ? "ON" : "OFF");
                int tw = SbeScreen.this.font.width(text);
                g.text(SbeScreen.this.font, text,
                        getX() - tw - 8, getY() + 6,
                        0xFFBFC0C9, false);
            }
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

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int aa = (a >>> 24) & 255;
        int ar = (a >>> 16) & 255;
        int ag = (a >>> 8) & 255;
        int ab = a & 255;
        int ba = (b >>> 24) & 255;
        int br = (b >>> 16) & 255;
        int bg = (b >>> 8) & 255;
        int bb = b & 255;
        int ca = aa + Math.round((ba - aa) * t);
        int cr = ar + Math.round((br - ar) * t);
        int cg = ag + Math.round((bg - ag) * t);
        int cb = ab + Math.round((bb - ab) * t);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
