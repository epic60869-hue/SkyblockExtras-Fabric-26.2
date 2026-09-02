package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;

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
    private int left, top, widthBox, heightBox;

    private boolean harvestExpanded = true;
    private boolean slugsExpanded = false;
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

        widthBox = Math.min(1180, this.width - 40);
        heightBox = Math.min(760, this.height - 32);
        left = (this.width - widthBox) / 2;
        top = (this.height - heightBox) / 2;

        // Decorative background. It is deliberately non-interactive.
        this.addRenderableWidget(new BackgroundWidget(left, top, widthBox, heightBox));

        // Sidebar.
        int sideX = left + 18;
        int sideY = top + 86;
        int sideW = 205;
        int sideH = heightBox - 145;

        this.addRenderableWidget(new PanelWidget(sideX, sideY, sideW, sideH));

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String selected = CATEGORIES[i];
            int y = sideY + 44 + i * 48;
            addCategoryButton(selected, sideX + 10, y, sideW - 20);
        }

        // Main content panel.
        int contentX = left + 238;
        int contentY = top + 86;
        int contentW = widthBox - 256;
        int contentH = heightBox - 145;
        this.addRenderableWidget(new PanelWidget(contentX, contentY, contentW, contentH));

        switch (category) {
            case "About" -> buildAbout(contentX, contentY, contentW);
            case "GUI" -> buildGui(contentX, contentY, contentW);
            case "Farming RNG" -> buildFarming(contentX, contentY, contentW);
            case "Pet" -> buildPet(contentX, contentY, contentW);
            default -> buildComingSoon(contentX, contentY, contentW);
        }

        addStyledButton("Done", left + widthBox - 132, top + heightBox - 48, 112, 28,
                true, this::onClose);
    }

    private void addCategoryButton(String name, int x, int y, int w) {
        addStyledButton(name, x, y, w, 38, name.equals(category), () -> {
            category = name;
            rebuildWidgets();
        });
    }

    private void buildAbout(int x, int y, int w) {
        addHeader("ABOUT", "Skyblock Extras client-side utilities", x + 22, y + 22);

        addCard(x + 20, y + 72, w - 40, 96, "Skyblock Extras",
                "A client-side SkyBlock utility mod for Minecraft 26.2.");
        addText("Version 0.1.2", x + 34, y + 126, 0xFFB45CFF);
        addText("Configure HUDs, farming RNG tracking and other utilities from this menu.",
                x + 34, y + 148, 0xFF9B9BA6);
    }

    private void buildGui(int x, int y, int w) {
        addHeader("GUI & HUD", "Configure overlays and the in-game position editor.", x + 22, y + 22);

        int yy = y + 78;
        addToggleCard("Pet Overlay", "Display the active pet information HUD.",
                SkyblockExtrasClient.CONFIG.petOverlayEnabled, x + 20, yy, w - 40,
                () -> SkyblockExtrasClient.CONFIG.petOverlayEnabled = !SkyblockExtrasClient.CONFIG.petOverlayEnabled);
        yy += 66;

        addCard(x + 20, yy, w - 40, 66, "Position / Size Editor",
                "Drag HUD elements and scroll to resize them.");
        addStyledButton("OPEN", x + w - 118, yy + 19, 82, 28, false,
                () -> Minecraft.getInstance().gui.setScreen(new SbeScreen(this)));
    }

    private void buildFarming(int x, int y, int w) {
        addHeader("FARMING RNG", "Track rare farming drops in chat with persistent timers.", x + 22, y + 22);

        int innerX = x + 20;
        int innerW = w - 40;
        int yy = y + 72;

        addToggleCard("Farming RNG", "Master switch for all farming RNG tracking.",
                SkyblockExtrasClient.CONFIG.farmingRngEnabled, innerX, yy, innerW,
                () -> SkyblockExtrasClient.CONFIG.farmingRngEnabled = !SkyblockExtrasClient.CONFIG.farmingRngEnabled);
        yy += 64;

        yy = addSection("HARVEST FEAST", "Track Harvest Feast drops.",
                SkyblockExtrasClient.CONFIG.harvestFeastEnabled, harvestExpanded,
                innerX, yy, innerW,
                () -> { harvestExpanded = !harvestExpanded; rebuildWidgets(); },
                () -> SkyblockExtrasClient.CONFIG.harvestFeastEnabled = !SkyblockExtrasClient.CONFIG.harvestFeastEnabled);

        if (harvestExpanded) {
            int columns = 2;
            int gap = 8;
            int colW = (innerW - gap) / columns;
            int rowH = 34;
            for (int i = 0; i < HARVEST_FEAST_DROPS.length; i++) {
                String item = HARVEST_FEAST_DROPS[i];
                int col = i % 2;
                int row = i / 2;
                int ix = innerX + col * (colW + gap);
                int iy = yy + row * rowH;
                boolean enabled = SkyblockExtrasClient.CONFIG.harvestFeastDrops.getOrDefault(item, true);
                addItemToggle(item, enabled, ix, iy, colW,
                        () -> SkyblockExtrasClient.CONFIG.harvestFeastDrops.put(item, !enabled));
            }
            yy += ((HARVEST_FEAST_DROPS.length + 1) / 2) * rowH + 10;
        }

        yy = addSection("SLUGS", "Track Epic and Legendary Slug drops.",
                SkyblockExtrasClient.CONFIG.slugEnabled, slugsExpanded,
                innerX, yy, innerW,
                () -> { slugsExpanded = !slugsExpanded; rebuildWidgets(); },
                () -> SkyblockExtrasClient.CONFIG.slugEnabled = !SkyblockExtrasClient.CONFIG.slugEnabled);

        if (slugsExpanded) {
            addItemToggle("Epic Slug", SkyblockExtrasClient.CONFIG.epicSlug,
                    innerX, yy, innerW, () -> SkyblockExtrasClient.CONFIG.epicSlug = !SkyblockExtrasClient.CONFIG.epicSlug);
            addItemToggle("Legendary Slug", SkyblockExtrasClient.CONFIG.legendarySlug,
                    innerX, yy + 36, innerW, () -> SkyblockExtrasClient.CONFIG.legendarySlug = !SkyblockExtrasClient.CONFIG.legendarySlug);
            yy += 76;
        }

        addSection("FARMING DYES", "Track farming-related dye drops.",
                SkyblockExtrasClient.CONFIG.dyesEnabled, dyesExpanded,
                innerX, yy, innerW,
                () -> { dyesExpanded = !dyesExpanded; rebuildWidgets(); },
                () -> SkyblockExtrasClient.CONFIG.dyesEnabled = !SkyblockExtrasClient.CONFIG.dyesEnabled);
    }

    private int addSection(String title, String description, boolean enabled, boolean expanded,
                           int x, int y, int w, Runnable expand, Runnable toggle) {
        addRenderableWidget(new SectionBackgroundWidget(x, y, w, expanded ? 40 : 54));
        String arrow = expanded ? "⌄" : "›";
        addStyledButton(arrow + "  " + title, x + 8, y + 7, 190, 32, false, expand);
        addText(description, x + 16, y + 39, 0xFF888894);
        addToggleButton("Enabled", enabled, x + w - 116, y + 11, 100,
                () -> { toggle.run(); rebuildWidgets(); });
        return y + (expanded ? 40 : 54);
    }

    private void buildPet(int x, int y, int w) {
        addHeader("PET OVERLAY", "Choose which information is visible in the pet HUD.", x + 22, y + 22);

        int yy = y + 72;
        int innerX = x + 20;
        int innerW = w - 40;
        SbeConfig c = SkyblockExtrasClient.CONFIG;

        addToggleCard("Pet Overlay", "Display the pet HUD.", c.petOverlayEnabled, innerX, yy, innerW,
                () -> c.petOverlayEnabled = !c.petOverlayEnabled); yy += 62;
        addToggleCard("Pet Icon", "Show the pet icon.", c.showPetIcon, innerX, yy, innerW,
                () -> c.showPetIcon = !c.showPetIcon); yy += 62;
        addToggleCard("Pet Level", "Show the current pet level.", c.showPetLevel, innerX, yy, innerW,
                () -> c.showPetLevel = !c.showPetLevel); yy += 62;
        addToggleCard("Pet Progress", "Show the XP progress bar.", c.showPetProgress, innerX, yy, innerW,
                () -> c.showPetProgress = !c.showPetProgress); yy += 62;
        addToggleCard("Pet XP", "Show total pet XP.", c.showPetXp, innerX, yy, innerW,
                () -> c.showPetXp = !c.showPetXp); yy += 62;
        addToggleCard("Overflow XP", "Show overflow XP.", c.showOverflowXp, innerX, yy, innerW,
                () -> c.showOverflowXp = !c.showOverflowXp); yy += 62;
        addToggleCard("Pet Item", "Show the held pet item.", c.showPetItem, innerX, yy, innerW,
                () -> c.showPetItem = !c.showPetItem);
    }

    private void buildComingSoon(int x, int y, int w) {
        addHeader(category.toUpperCase(), "Settings for this category are coming next.", x + 22, y + 22);
        addCard(x + 20, y + 72, w - 40, 70, category,
                "This section is ready for the next SBE feature set.");
    }

    private void addHeader(String title, String subtitle, int x, int y) {
        addText(title, x, y, 0xFFF2F2F6);
        addText(subtitle, x, y + 26, 0xFF9898A3);
    }

    private void addCard(int x, int y, int w, int h, String title, String description) {
        addRenderableWidget(new CardWidget(x, y, w, h));
        addText(title, x + 14, y + 14, 0xFFE8E8EE);
        if (!description.isEmpty()) addText(description, x + 14, y + 38, 0xFF92929D);
    }

    private void addToggleCard(String title, String description, boolean enabled,
                               int x, int y, int w, Runnable action) {
        addCard(x, y, w, 54, title, description);
        addToggleButton("", enabled, x + w - 104, y + 13, 90, action);
    }

    private void addItemToggle(String name, boolean enabled, int x, int y, int w, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 32));
        addText(name, x + 12, y + 9, 0xFFDCDCE3);
        addToggleButton("", enabled, x + w - 82, y + 4, 70, action);
    }

    private void addToggleButton(String label, boolean enabled, int x, int y, int w, Runnable action) {
        addStyledButton((label.isEmpty() ? "" : label + "  ") + (enabled ? "ON" : "OFF"),
                x, y, w, 24, enabled, action);
    }

    private void addStyledButton(String text, int x, int y, int w, int h, boolean selected, Runnable action) {
        this.addRenderableWidget(new StyledButton(x, y, w, h, Component.literal(text), selected, action));
    }

    private void addText(String text, int x, int y, int color) {
        this.addRenderableWidget(new LabelWidget(x, y, text, color));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private class BackgroundWidget extends AbstractWidget {
        BackgroundWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(0, 0, SbeScreen.this.width, SbeScreen.this.height, 0xFF08090D);
            graphics.fill(left - 1, top - 1, left + widthBox + 1, top + heightBox + 1, 0xFF0D0E14);
            graphics.outline(left - 1, top - 1, widthBox + 2, heightBox + 2, 0xFF7C35C7);

            graphics.fill(left, top, left + widthBox, top + 62, 0xFF101117);
            graphics.outline(left, top, widthBox, 62, 0xFF252733);

            graphics.text(SbeScreen.this.font, "SKYBLOCK EXTRAS", left + 24, top + 19, 0xFFF2F2F6, false);
            graphics.text(SbeScreen.this.font, "0.1.2", left + 180, top + 19, 0xFFB45CFF, false);
            graphics.text(SbeScreen.this.font, "— configuration", left + 216, top + 19, 0xFF8D8D98, false);

            graphics.text(SbeScreen.this.font, "SBE", left + widthBox - 55, top + 20, 0xFFB45CFF, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    private class PanelWidget extends AbstractWidget {
        PanelWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF0D0F15);
            graphics.outline(getX(), getY(), width, height, 0xFF282A36);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    private class SectionBackgroundWidget extends AbstractWidget {
        SectionBackgroundWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF141620);
            graphics.outline(getX(), getY(), width, height, 0xFF292B38);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    private class CardWidget extends AbstractWidget {
        CardWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF151720);
            graphics.outline(getX(), getY(), width, height, 0xFF272A36);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    private class LabelWidget extends AbstractWidget {
        private final String text;
        private final int color;

        LabelWidget(int x, int y, String text, int color) {
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

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    private class StyledButton extends AbstractWidget {
        private final Runnable action;
        private final boolean selected;

        StyledButton(int x, int y, int width, int height, Component message, boolean selected, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
            this.selected = selected;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            int fill = selected ? 0xFF241B32 : (hovered ? 0xFF202230 : 0xFF171922);
            int border = selected ? 0xFFB45CFF : (hovered ? 0xFF7440A0 : 0xFF303341);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            graphics.outline(getX(), getY(), width, height, border);
            if (selected) graphics.fill(getX(), getY(), getX() + 3, getY() + height, 0xFFB45CFF);

            int textWidth = SbeScreen.this.font.width(getMessage());
            int tx = getX() + (width - textWidth) / 2;
            int ty = getY() + (height - 8) / 2;
            graphics.text(SbeScreen.this.font, getMessage(), tx, ty,
                    selected ? 0xFFF4E8FF : 0xFFD9D9E2, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            defaultButtonNarrationText(builder);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
