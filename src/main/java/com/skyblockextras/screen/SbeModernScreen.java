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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Skysoft-inspired configuration screen for Skyblock Extras. */
public class SbeModernScreen extends Screen {
    private final Screen parent;
    private final String category;
    private int left, top, widthPanel, heightPanel;

    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    private final List<Integer> scrollBaseY = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int clipTop = 0;
    private int clipBottom = 0;
    private int clipLeft = 0;
    private int clipRight = 0;

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

    @Override
    protected void init() {
        super.init();
        widthPanel = Math.min(1000, width - 32);
        heightPanel = Math.min(680, height - 32);
        left = (width - widthPanel) / 2;
        top = (height - heightPanel) / 2;
        scrollWidgets.clear();
        scrollBaseY.clear();
        scrollOffset = 0;

        addRenderableWidget(new BackgroundWidget());
        int sidebarX = left + 14;
        int sidebarY = top + 64;
        int sidebarW = 190;
        int sidebarH = heightPanel - 78;
        addRenderableWidget(new PanelWidget(sidebarX, sidebarY, sidebarW, sidebarH, 0));
        addLabel("SKYBLOCK EXTRAS", sidebarX + 14, sidebarY + 14, 0xFFFFFFFF, true);
        addLabel("CONFIGURATION", sidebarX + 14, sidebarY + 29, 0xFF8D8E99, false);

        for (int i = 0; i < CATEGORIES.length; i++) {
            String selected = CATEGORIES[i];
            addRenderableWidget(new NavButton(sidebarX + 9, sidebarY + 54 + i * 42, sidebarW - 18, 34,
                    selected, selected.equals(category),
                    () -> Minecraft.getInstance().gui.setScreen(new SbeModernScreen(parent, selected))));
        }

        int contentX = sidebarX + sidebarW + 12;
        int contentY = sidebarY;
        int contentW = widthPanel - sidebarW - 40;
        int contentH = sidebarH;
        addRenderableWidget(new PanelWidget(contentX, contentY, contentW, contentH, 1));

        switch (category) {
            case "GUI" -> buildGui(contentX, contentY, contentW, contentH);
            case "Farming RNG" -> buildFarming(contentX, contentY, contentW, contentH);
            case "Pet" -> buildPet(contentX, contentY, contentW, contentH);
            default -> buildAbout(contentX, contentY, contentW, contentH);
        }

        setupContentScrolling(contentX, contentY + 62, contentW, contentH - 70);
        addRenderableWidget(new BottomButton(left + widthPanel - 105, top + heightPanel - 38, 88, 25, "Done", this::onClose));
    }

    private void setupContentScrolling(int x, int y, int w, int h) {
        clipLeft = x;
        clipRight = x + w;
        clipTop = y;
        clipBottom = y + h;

        int maxBottom = clipTop;
        for (Object child : children()) {
            if (!(child instanceof AbstractWidget widget)) continue;
            int wx = widget.getX();
            int wy = widget.getY();
            if (wx >= x && wx < x + w && wy >= y) {
                scrollWidgets.add(widget);
                scrollBaseY.add(wy);
                maxBottom = Math.max(maxBottom, wy + widget.getHeight());
            }
        }

        maxScroll = Math.max(0, maxBottom - clipBottom);
        applyScroll();
    }

    private void applyScroll() {
        for (int i = 0; i < scrollWidgets.size(); i++) {
            AbstractWidget widget = scrollWidgets.get(i);
            int newY = scrollBaseY.get(i) - scrollOffset;
            widget.setY(newY);
            widget.visible = newY + widget.getHeight() > clipTop && newY < clipBottom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseX >= clipLeft && mouseX <= clipRight && mouseY >= clipTop && mouseY <= clipBottom) {
            int amount = (int) Math.round(verticalAmount * 24.0);
            if (amount != 0) {
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - amount));
                applyScroll();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void buildAbout(int x, int y, int w, int h) {
        addTitle(x, y, "About", "Skyblock Extras configuration");
        addCard(x + 16, y + 72, w - 32, 106, "Skyblock Extras", "A lightweight client-side SkyBlock utility mod.", "v0.1.2");
        addCard(x + 16, y + 190, w - 32, 106, "Modern configuration", "Manage your HUD, farming RNG tracking and pet display from one place.", "FABRIC 26.2");
        addCard(x + 16, y + 308, w - 32, 106, "HUD editor", "Drag overlays and use the mouse wheel to resize them.", "HUD");
    }

    private void buildGui(int x, int y, int w, int h) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        addTitle(x, y, "GUI", "HUD visibility, background, scale and positioning");
        int cy = y + 69;
        addSetting(x + 16, cy, w - 32, 48, "Pet Overlay", "Display the active pet HUD.", () -> c.petOverlayEnabled,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled));
        cy += 56;
        addSetting(x + 16, cy, w - 32, 48, "Pet Background", "Show the dark panel behind the pet HUD.", () -> c.petBackgroundEnabled,
                () -> toggle(() -> c.petBackgroundEnabled = !c.petBackgroundEnabled));
        cy += 56;
        addSetting(x + 16, cy, w - 32, 48, "RNG Drop Overlay", "Show a notification when a tracked RNG drop is found.", () -> c.rngDropOverlayEnabled,
                () -> toggle(() -> c.rngDropOverlayEnabled = !c.rngDropOverlayEnabled));
        cy += 56;
        addSetting(x + 16, cy, w - 32, 48, "RNG Background", "Show the panel behind RNG drop announcements.", () -> c.rngDropOverlayBackgroundEnabled,
                () -> toggle(() -> c.rngDropOverlayBackgroundEnabled = !c.rngDropOverlayBackgroundEnabled));
        cy += 64;
        addEditorCard(x + 16, cy, w - 32, "Pet Overlay Position & Scale", "Drag the pet HUD and use the mouse wheel to resize it.",
                "EDIT", () -> Minecraft.getInstance().gui.setScreen(new PositionEditorScreen(this)));
        cy += 72;
        addEditorCard(x + 16, cy, w - 32, "RNG Drop Position & Scale", "Drag the RNG announcement and use the mouse wheel to resize it.",
                "EDIT", () -> Minecraft.getInstance().gui.setScreen(new RngDropPositionEditorScreen(this)));
        cy += 72;
        addCycleCard(x + 16, cy, w - 32, "RNG Price Formatting", "Choose how the drop price is displayed.",
                () -> priceFormatLabel(c.rngDropPriceFormat), () -> cyclePriceFormat(c));
    }

    private String priceFormatLabel(String format) {
        if (format == null) return "SHORT";
        return switch (format.toUpperCase()) {
            case "FULL" -> "FULL";
            case "COINS" -> "COINS";
            default -> "SHORT";
        };
    }

    private void cyclePriceFormat(SbeConfig c) {
        String current = priceFormatLabel(c.rngDropPriceFormat);
        c.rngDropPriceFormat = switch (current) {
            case "SHORT" -> "FULL";
            case "FULL" -> "COINS";
            default -> "SHORT";
        };
        c.save();
    }

    private void buildFarming(int x, int y, int w, int h) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        addTitle(x, y, "Farming RNG", "Persistent timers for selected rare farming drops");
        int cy = y + 69;
        addSetting(x + 16, cy, w - 32, 48, "Farming RNG", "Master switch for all farming RNG tracking.", () -> c.farmingRngEnabled,
                () -> toggle(() -> c.farmingRngEnabled = !c.farmingRngEnabled));
        cy += 56;
        addSetting(x + 16, cy, w - 32, 42, "Harvest Feast", "Track only the configured Harvest Feast drops.", () -> c.harvestFeastEnabled,
                () -> toggle(() -> c.harvestFeastEnabled = !c.harvestFeastEnabled));
        cy += 49;

        int gridX = x + 16;
        int gridW = w - 32;
        int gap = 6;
        int colW = (gridW - gap) / 2;
        int rowH = 23;
        int listTop = cy;
        int rows = (HARVEST.length + 1) / 2;
        for (int i = 0; i < HARVEST.length; i++) {
            String item = HARVEST[i];
            int col = i % 2;
            int row = i / 2;
            addSmallToggle(gridX + col * (colW + gap), listTop + row * rowH, colW, item,
                    () -> c.harvestFeastDrops.getOrDefault(item, true),
                    () -> toggle(() -> c.harvestFeastDrops.put(item, !c.harvestFeastDrops.getOrDefault(item, true))));
        }
        cy = listTop + rows * rowH + 10;
        addSetting(x + 16, cy, w - 32, 42, "Slugs", "Enable slug RNG tracking.", () -> c.slugEnabled,
                () -> toggle(() -> c.slugEnabled = !c.slugEnabled));
        cy += 47;
        addSmallToggle(x + 16, cy, (w - 38) / 2, "Epic Slug", () -> c.epicSlug,
                () -> toggle(() -> c.epicSlug = !c.epicSlug));
        addSmallToggle(x + 22 + (w - 38) / 2, cy, (w - 38) / 2, "Legendary Slug", () -> c.legendarySlug,
                () -> toggle(() -> c.legendarySlug = !c.legendarySlug));
        cy += 28;
        addSetting(x + 16, cy, w - 32, 42, "Dyes", "Enable farming-related dye tracking.", () -> c.dyesEnabled,
                () -> toggle(() -> c.dyesEnabled = !c.dyesEnabled));
    }

    private void buildPet(int x, int y, int w, int h) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        addTitle(x, y, "Pet", "Configure the active pet overlay");
        int cy = y + 69;
        addSetting(x + 16, cy, w - 32, 44, "Pet Overlay", "Display the active pet information.", () -> c.petOverlayEnabled,
                () -> toggle(() -> c.petOverlayEnabled = !c.petOverlayEnabled));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Pet Background", "Show the HUD background panel.", () -> c.petBackgroundEnabled,
                () -> toggle(() -> c.petBackgroundEnabled = !c.petBackgroundEnabled));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Pet Icon", "Show the pet icon.", () -> c.showPetIcon,
                () -> toggle(() -> c.showPetIcon = !c.showPetIcon));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Pet Level", "Show the pet level and rarity.", () -> c.showPetLevel,
                () -> toggle(() -> c.showPetLevel = !c.showPetLevel));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Pet Progress", "Show the current pet XP progress bar.", () -> c.showPetProgress,
                () -> toggle(() -> c.showPetProgress = !c.showPetProgress));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Pet XP", "Show total pet XP.", () -> c.showPetXp,
                () -> toggle(() -> c.showPetXp = !c.showPetXp));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Overflow XP", "Show XP beyond the pet's maximum level.", () -> c.showOverflowXp,
                () -> toggle(() -> c.showOverflowXp = !c.showOverflowXp));
        cy += 51;
        addSetting(x + 16, cy, w - 32, 44, "Pet Item", "Show the held pet item.", () -> c.showPetItem,
                () -> toggle(() -> c.showPetItem = !c.showPetItem));
    }

    private void addTitle(int x, int y, String title, String subtitle) {
        addLabel(title, x + 18, y + 17, 0xFFF2F2F5, true);
        addLabel(subtitle, x + 18, y + 38, 0xFF9697A3, false);
        addRenderableWidget(new RuleWidget(x + 16, y + 58, 1, 0));
    }

    private void addCard(int x, int y, int w, int h, String title, String desc, String badge) {
        addRenderableWidget(new CardWidget(x, y, w, h));
        addLabel(title, x + 16, y + 18, 0xFFE7E7EC, true);
        addLabel(desc, x + 16, y + 42, 0xFF9B9CA6, false);
        addRenderableWidget(new BadgeWidget(x + w - 105, y + 16, 89, 24, badge));
    }

    private void addSetting(int x, int y, int w, int h, String title, String desc, BooleanSupplier value, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, h));
        addLabel(title, x + 14, y + 8, 0xFFE4E4E9, true);
        addLabel(desc, x + 14, y + 27, 0xFF9697A2, false);
        addRenderableWidget(new ToggleButton(x + w - 62, y + 12, 47, 23, value, action));
    }

    private void addSmallToggle(int x, int y, int w, String title, BooleanSupplier value, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 21));
        addLabel(title, x + 8, y + 5, 0xFFD6D6DC, false);
        addRenderableWidget(new ToggleButton(x + w - 39, y + 2, 31, 17, value, action));
    }

    private void addEditorCard(int x, int y, int w, String title, String desc, String button, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 62));
        addLabel(title, x + 14, y + 12, 0xFFE5E5EA, true);
        addLabel(desc, x + 14, y + 34, 0xFF9798A3, false);
        addRenderableWidget(new BottomButton(x + w - 76, y + 19, 61, 24, button, action));
    }

    private void addCycleCard(int x, int y, int w, String title, String desc, java.util.function.Supplier<String> value, Runnable action) {
        addRenderableWidget(new CardWidget(x, y, w, 62));
        addLabel(title, x + 14, y + 12, 0xFFE5E5EA, true);
        addLabel(desc, x + 14, y + 34, 0xFF9798A3, false);
        addRenderableWidget(new BottomButton(x + w - 76, y + 19, 61, 24, value.get(), action));
    }

    private void addLabel(String text, int x, int y, int color, boolean bold) {
        addRenderableWidget(new LabelWidget(x, y, text, color, bold));
    }

    private void toggle(Runnable action) {
        action.run();
        if (SkyblockExtrasClient.CONFIG != null) SkyblockExtrasClient.CONFIG.save();
    }

    private class BackgroundWidget extends AbstractWidget {
        BackgroundWidget() { super(0, 0, SbeModernScreen.this.width, SbeModernScreen.this.height, Component.empty()); active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xB807080C);
            g.fill(left - 2, top - 2, left + widthPanel + 2, top + heightPanel + 2, 0xFF090A0F);
            g.fill(left, top, left + widthPanel, top + heightPanel, 0xFF15161B);
            g.fill(left + 1, top + 1, left + widthPanel - 1, top + 57, 0xFF1B1C22);
            g.outline(left, top, widthPanel, heightPanel, 0xFF383943);
            g.horizontalLine(left + 1, left + widthPanel - 1, top + 57, 0xFF34353C);
            g.text(font, "Skyblock Extras", left + 20, top + 18, 0xFFF0F0F3, true);
            g.text(font, "CONFIGURATION", left + 20, top + 36, 0xFF999AA5, false);
            g.text(font, "0.1.2", left + widthPanel - 50, top + 25, 0xFFB96BFF, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class PanelWidget extends AbstractWidget {
        PanelWidget(int x, int y, int w, int h, int ignored) { super(x, y, w, h, Component.empty()); active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF17181E);
            g.outline(getX(), getY(), width, height, 0xFF303139);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class CardWidget extends AbstractWidget {
        CardWidget(int x, int y, int w, int h) { super(x, y, w, h, Component.empty()); active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF1D1E24);
            g.outline(getX(), getY(), width, height, 0xFF2C2D35);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class RuleWidget extends AbstractWidget {
        RuleWidget(int x, int y, int w, int h) { super(x, y, 1, 1, Component.empty()); active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) { }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class LabelWidget extends AbstractWidget {
        private final int color;
        private final boolean bold;
        LabelWidget(int x, int y, String text, int color, boolean bold) {
            super(x, y, font.width(text) + 3, 10, Component.literal(text));
            this.color = color;
            this.bold = bold;
            active = false;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.text(font, getMessage(), getX(), getY(), color, bold);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class BadgeWidget extends AbstractWidget {
        BadgeWidget(int x, int y, int w, int h, String text) { super(x, y, w, h, Component.literal(text)); active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF252631);
            g.outline(getX(), getY(), width, height, 0xFF3B3C48);
            int tw = font.width(getMessage());
            g.text(font, getMessage(), getX() + (width - tw) / 2, getY() + 8, 0xFFB96BFF, false);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class NavButton extends AbstractWidget {
        private final boolean selected;
        private final Runnable action;
        NavButton(int x, int y, int w, int h, String text, boolean selected, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.selected = selected;
            this.action = action;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hover = isMouseOver(mx, my);
            int bg = selected ? 0xFF55306D : hover ? 0xFF292A32 : 0xFF202128;
            int border = selected ? 0xFFB96BFF : 0xFF34353D;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.outline(getX(), getY(), width, height, border);
            g.fill(getX(), getY(), getX() + 3, getY() + height, selected ? 0xFFB96BFF : bg);
            g.text(font, getMessage(), getX() + 13, getY() + 11, selected ? 0xFFFFFFFF : 0xFFD5D5DC, selected);
        }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class ToggleButton extends AbstractWidget {
        private final BooleanSupplier value;
        private final Runnable action;
        ToggleButton(int x, int y, int w, int h, BooleanSupplier value, Runnable action) {
            super(x, y, w, h, Component.empty());
            this.value = value;
            this.action = action;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean on = value.getAsBoolean();
            boolean hover = isMouseOver(mx, my);
            int bg = on ? 0xFF7A45A1 : (hover ? 0xFF30313A : 0xFF272830);
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.outline(getX(), getY(), width, height, on ? 0xFFB96BFF : 0xFF3D3E48);
            int knob = Math.max(8, height - 7);
            int kx = on ? getX() + width - knob - 3 : getX() + 3;
            int ky = getY() + (height - knob) / 2;
            g.fill(kx, ky, kx + knob, ky + knob, 0xFFEDEDF0);
        }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }

    private class BottomButton extends AbstractWidget {
        private final Runnable action;
        BottomButton(int x, int y, int w, int h, String text, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.action = action;
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hover = isMouseOver(mx, my);
            g.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF75439A : 0xFF623580);
            g.outline(getX(), getY(), width, height, 0xFFB96BFF);
            int tw = font.width(getMessage());
            g.text(font, getMessage(), getX() + (width - tw) / 2, getY() + 8, 0xFFFFFFFF, false);
        }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }
}
