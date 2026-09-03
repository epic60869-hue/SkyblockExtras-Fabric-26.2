package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** SkyHanni-inspired SBE configuration screen with properly clipped scrolling. */
public class SbeSkyHanniScreen extends Screen {
    private static final String[] CATEGORIES = {"About", "GUI", "Farming RNG", "Pet"};
    private static final String[] HARVEST = {
            "Aggourdian", "Botroot", "Cactus Flower", "Cane Knot", "Carrot Zest", "Cornucopia", "Cropie",
            "Crystalized Moonlight", "Deepfries", "Designer Coffee Beans", "Feastfungus", "Fermento",
            "Floral Gelatin", "Helianthus", "Melon Juice", "Salted Sunflower Seeds", "Squash"
    };

    private final Screen parent;
    private int selected;
    private int left, top, panelW, panelH;
    private int sidebarLeft, sidebarTop, sidebarWidth;
    private int contentLeft, contentTop, contentRight, contentBottom;
    private int scroll, maxScroll;

    public SbeSkyHanniScreen(Screen parent) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(1120, width - 32);
        panelH = Math.min(720, height - 32);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        sidebarLeft = left + 14;
        sidebarTop = top + 68;
        sidebarWidth = 210;
        contentLeft = sidebarLeft + sidebarWidth + 18;
        contentTop = top + 68;
        contentRight = left + panelW - 18;
        contentBottom = top + panelH - 54;
        recalcScroll();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xB508090D);
        g.fill(left, top, left + panelW, top + panelH, 0xFF15161B);
        g.outline(left, top, left + panelW, top + panelH, 0xFF30313A);
        g.text(font, Component.literal("SKYBLOCK EXTRAS"), left + 22, top + 17, 0xFFF1F1F4, true);
        g.text(font, Component.literal("Configuration"), left + 22, top + 34, 0xFF858690, false);

        drawSidebar(g, mouseX, mouseY);
        g.fill(contentLeft, contentTop, contentRight, contentBottom, 0xFF0F1014);
        g.enableScissor(contentLeft, contentTop, contentRight, contentBottom);
        drawContent(g, mouseX, mouseY);
        g.disableScissor();
        drawScrollbar(g);

        int doneX = left + panelW - 106;
        int doneY = top + panelH - 40;
        button(g, doneX, doneY, 88, 26, "Done", mouseX >= doneX && mouseX <= doneX + 88 && mouseY >= doneY && mouseY <= doneY + 26);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int h = panelH - 132;
        g.fill(sidebarLeft, sidebarTop, sidebarLeft + sidebarWidth, sidebarTop + h, 0xFF101116);
        g.outline(sidebarLeft, sidebarTop, sidebarLeft + sidebarWidth, sidebarTop + h, 0xFF292A31);
        g.text(font, Component.literal("FEATURES"), sidebarLeft + 16, sidebarTop + 15, 0xFF777883, true);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = sidebarTop + 40 + i * 45;
            boolean active = selected == i;
            boolean hover = mouseX >= sidebarLeft + 7 && mouseX <= sidebarLeft + sidebarWidth - 7 && mouseY >= y && mouseY <= y + 34;
            if (active) g.fill(sidebarLeft + 7, y, sidebarLeft + sidebarWidth - 7, y + 34, 0xFF2B2034);
            else if (hover) g.fill(sidebarLeft + 7, y, sidebarLeft + sidebarWidth - 7, y + 34, 0xFF1C1D23);
            g.text(font, Component.literal(CATEGORIES[i]), sidebarLeft + 20, y + 10, active ? 0xFFC77DFF : 0xFFD4D4DA, false);
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor g) {
        if (maxScroll <= 0) return;
        int x = contentRight - 5;
        int a = contentTop + 8;
        int b = contentBottom - 8;
        int track = b - a;
        g.fill(x, a, x + 3, b, 0xFF25262D);
        int thumb = Math.max(26, Math.round(track * (contentBottom - contentTop) / (float) contentHeight()));
        int travel = Math.max(0, track - thumb);
        int y = a + Math.round(travel * (scroll / (float) maxScroll));
        g.fill(x, y, x + 3, y + thumb, 0xFF77707F);
    }

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = contentTop + 18 - scroll;
        String subtitle = switch (selected) {
            case 1 -> "HUD elements and visual positioning";
            case 2 -> "Persistent timers for rare farming drops";
            case 3 -> "Active pet display and overflow XP";
            default -> "Skyblock Extras configuration";
        };
        text(g, CATEGORIES[selected], contentLeft + 20, y, 0xFFF0F0F4, true);
        text(g, subtitle, contentLeft + 20, y + 20, 0xFF898A95, false);
        g.fill(contentLeft + 18, y + 42, contentRight - 18, y + 43, 0xFF2B2C34);
        y += 58;

        if (selected == 0) {
            info(g, y, "Skyblock Extras", "Client-side SkyBlock utilities for Minecraft 26.2.", "v0.1.2");
            info(g, y + 104, "Configuration", "Use the categories on the left to configure each feature.", "SBE");
            info(g, y + 208, "GUI controls", "Drag overlays to reposition them and use the mouse wheel to scale.", "READY");
            return;
        }

        SbeConfig c = SkyblockExtrasClient.CONFIG;
        if (selected == 1) {
            row(g, y, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled); y += 56;
            row(g, y, "Pet Background", "Show the background behind the pet display.", c.petBackgroundEnabled); y += 56;
            row(g, y, "RNG Drop Overlay", "Show a notification when a tracked RNG drop occurs.", c.rngDropOverlayEnabled); y += 56;
            row(g, y, "RNG Background", "Show the background behind RNG announcements.", c.rngDropOverlayBackgroundEnabled); y += 56;
            actionRow(g, y, "Pet Position & Scale", "Open the drag/scale editor.", "EDIT", mouseX, mouseY); y += 60;
            actionRow(g, y, "RNG Position & Scale", "Open the drag/scale editor.", "EDIT", mouseX, mouseY); y += 60;
            choiceRow(g, y, "RNG Price Formatting", "Click to cycle between price formats.", priceLabel(c.rngDropPriceFormat));
            return;
        }

        if (selected == 2) {
            row(g, y, "Farming RNG", "Master switch for farming RNG tracking.", c.farmingRngEnabled); y += 56;
            row(g, y, "Harvest Feast", "Track only the configured Feast drops.", c.harvestFeastEnabled); y += 56;
            text(g, "HARVEST FEAST DROPS", contentLeft + 22, y + 4, 0xFF777883, true); y += 26;
            int gridW = contentRight - contentLeft - 48;
            int colW = (gridW - 8) / 2;
            for (int i = 0; i < HARVEST.length; i++) {
                int col = i % 2, rr = i / 2;
                boolean on = c.harvestFeastDrops.getOrDefault(HARVEST[i], true);
                pill(g, contentLeft + 22 + col * (colW + 8), y + rr * 29, colW, HARVEST[i], on);
            }
            y += ((HARVEST.length + 1) / 2) * 29 + 12;
            row(g, y, "Slugs", "Track Epic and Legendary Slug drops.", c.slugEnabled); y += 56;
            row(g, y, "Epic Slug", "Track Epic Slug.", c.epicSlug); y += 56;
            row(g, y, "Legendary Slug", "Track Legendary Slug.", c.legendarySlug); y += 56;
            row(g, y, "Dyes", "Track configured farming-related dyes.", c.dyesEnabled);
            return;
        }

        row(g, y, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled); y += 56;
        row(g, y, "Pet Background", "Toggle the HUD background.", c.petBackgroundEnabled); y += 56;
        row(g, y, "Pet Icon", "Show the pet icon.", c.showPetIcon); y += 56;
        row(g, y, "Pet Level", "Show the combined level, including overflow.", c.showPetLevel); y += 56;
        row(g, y, "Level Progress", "Show progress to the next normal level.", c.showPetProgress); y += 56;
        row(g, y, "Pet XP", "Show total pet XP.", c.showPetXp); y += 56;
        row(g, y, "Pet Item", "Show the currently equipped pet item.", c.showPetItem);
    }

    private void row(GuiGraphicsExtractor g, int y, String title, String desc, boolean value) {
        card(g, y, 48);
        text(g, title, contentLeft + 30, y + 8, 0xFFE5E5EA, true);
        text(g, desc, contentLeft + 30, y + 26, 0xFF898A95, false);
        toggle(g, contentRight - 62, y + 13, value);
    }

    private void actionRow(GuiGraphicsExtractor g, int y, String title, String desc, String label, int mouseX, int mouseY) {
        card(g, y, 52);
        text(g, title, contentLeft + 30, y + 9, 0xFFE5E5EA, true);
        text(g, desc, contentLeft + 30, y + 28, 0xFF898A95, false);
        int bx = contentRight - 82;
        button(g, bx, y + 12, 68, 27, label, mouseX >= bx && mouseX <= bx + 68 && mouseY >= y + scroll + 12 && mouseY <= y + scroll + 39);
    }

    private void choiceRow(GuiGraphicsExtractor g, int y, String title, String desc, String value) {
        card(g, y, 52);
        text(g, title, contentLeft + 30, y + 9, 0xFFE5E5EA, true);
        text(g, desc, contentLeft + 30, y + 28, 0xFF898A95, false);
        text(g, value, contentRight - 88, y + 19, 0xFFC77DFF, false);
    }

    private void info(GuiGraphicsExtractor g, int y, String title, String desc, String badge) {
        card(g, y, 92);
        text(g, title, contentLeft + 30, y + 17, 0xFFE7E7EC, true);
        text(g, desc, contentLeft + 30, y + 42, 0xFF92939D, false);
        g.fill(contentRight - 94, y + 14, contentRight - 14, y + 39, 0xFF27202F);
        text(g, badge, contentRight - 79, y + 21, 0xFFC77DFF, false);
    }

    private void card(GuiGraphicsExtractor g, int y, int h) {
        if (y + h < contentTop || y > contentBottom) return;
        g.fill(contentLeft + 16, y, contentRight - 16, y + h, 0xFF1E1F25);
        g.outline(contentLeft + 16, y, contentRight - 16, y + h, 0xFF30313A);
    }

    private void toggle(GuiGraphicsExtractor g, int x, int y, boolean on) {
        g.fill(x, y, x + 38, y + 20, on ? 0xFF57376C : 0xFF292A30);
        g.outline(x, y, x + 38, y + 20, on ? 0xFF8C58A9 : 0xFF45464F);
        g.fill(on ? x + 21 : x + 3, y + 3, on ? x + 35 : x + 17, y + 17, on ? 0xFFE1C9EC : 0xFF85868E);
    }

    private void pill(GuiGraphicsExtractor g, int x, int y, int w, String name, boolean on) {
        if (y + 23 < contentTop || y > contentBottom) return;
        g.fill(x, y, x + w, y + 23, 0xFF1A1B20);
        g.outline(x, y, x + w, y + 23, on ? 0xFF68477B : 0xFF2D2E35);
        text(g, name, x + 8, y + 7, on ? 0xFFDCC5E8 : 0xFF777881, false);
        g.fill(x + w - 17, y + 7, x + w - 8, y + 16, on ? 0xFFC77DFF : 0xFF55565E);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? 0xFF3A2947 : 0xFF30243B);
        g.outline(x, y, x + w, y + h, 0xFF69477F);
        g.text(font, Component.literal(label), x + (w - font.width(label)) / 2, y + (h - 9) / 2, 0xFFE5D5ED, false);
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color, boolean shadow) {
        if (y >= contentTop - 14 && y <= contentBottom + 8) g.text(font, Component.literal(value), x, y, color, shadow);
    }

    private int contentHeight() {
        return switch (selected) {
            case 1 -> 58 + 4 * 56 + 2 * 60 + 52;
            case 2 -> 58 + 2 * 56 + 26 + ((HARVEST.length + 1) / 2) * 29 + 12 + 4 * 56;
            case 3 -> 58 + 7 * 56;
            default -> 58 + 3 * 104;
        };
    }

    private void recalcScroll() {
        maxScroll = Math.max(0, contentHeight() - (contentBottom - contentTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= contentLeft && mouseX <= contentRight && mouseY >= contentTop && mouseY <= contentBottom && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * 32.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() != 0) return super.mouseClicked(e, doubleClick);
        double mx = e.x(), my = e.y();

        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = sidebarTop + 40 + i * 45;
            if (mx >= sidebarLeft + 7 && mx <= sidebarLeft + sidebarWidth - 7 && my >= y && my <= y + 34) {
                selected = i;
                scroll = 0;
                recalcScroll();
                return true;
            }
        }

        int doneX = left + panelW - 106, doneY = top + panelH - 40;
        if (mx >= doneX && mx <= doneX + 88 && my >= doneY && my <= doneY + 26) {
            onClose();
            return true;
        }

        if (mx < contentLeft || mx > contentRight || my < contentTop || my > contentBottom) return false;
        double cy = my + scroll;
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int y = contentTop + 76;

        if (selected == 1) {
            if (hit(y, cy, 48)) { c.petOverlayEnabled = !c.petOverlayEnabled; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.petBackgroundEnabled = !c.petBackgroundEnabled; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.rngDropOverlayEnabled = !c.rngDropOverlayEnabled; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.rngDropOverlayBackgroundEnabled = !c.rngDropOverlayBackgroundEnabled; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 52)) { Minecraft.getInstance().gui.setScreen(new PositionEditorScreen(this)); return true; }
            y += 60;
            if (hit(y, cy, 52)) { Minecraft.getInstance().gui.setScreen(new RngDropPositionEditorScreen(this)); return true; }
            y += 60;
            if (hit(y, cy, 52)) { cyclePrice(c); return true; }
        } else if (selected == 2) {
            if (hit(y, cy, 48)) { c.farmingRngEnabled = !c.farmingRngEnabled; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.harvestFeastEnabled = !c.harvestFeastEnabled; c.save(); return true; }
            y += 56 + 26;
            int gridW = contentRight - contentLeft - 48, colW = (gridW - 8) / 2;
            for (int i = 0; i < HARVEST.length; i++) {
                int col = i % 2, rr = i / 2, xx = contentLeft + 22 + col * (colW + 8), yy = y + rr * 29;
                if (mx >= xx && mx <= xx + colW && cy >= yy && cy <= yy + 23) {
                    c.harvestFeastDrops.put(HARVEST[i], !c.harvestFeastDrops.getOrDefault(HARVEST[i], true));
                    c.save();
                    return true;
                }
            }
            y += ((HARVEST.length + 1) / 2) * 29 + 12;
            if (hit(y, cy, 48)) { c.slugEnabled = !c.slugEnabled; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.epicSlug = !c.epicSlug; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.legendarySlug = !c.legendarySlug; c.save(); return true; }
            y += 56;
            if (hit(y, cy, 48)) { c.dyesEnabled = !c.dyesEnabled; c.save(); return true; }
        } else if (selected == 3) {
            for (int i = 0; i < 7; i++) {
                if (!hit(y + i * 56, cy, 48)) continue;
                switch (i) {
                    case 0 -> c.petOverlayEnabled = !c.petOverlayEnabled;
                    case 1 -> c.petBackgroundEnabled = !c.petBackgroundEnabled;
                    case 2 -> c.showPetIcon = !c.showPetIcon;
                    case 3 -> c.showPetLevel = !c.showPetLevel;
                    case 4 -> c.showPetProgress = !c.showPetProgress;
                    case 5 -> c.showPetXp = !c.showPetXp;
                    case 6 -> c.showPetItem = !c.showPetItem;
                }
                c.save();
                return true;
            }
        }
        return false;
    }

    private boolean hit(double y, double my, double h) { return my >= y && my <= y + h; }

    private void cyclePrice(SbeConfig c) {
        c.rngDropPriceFormat = switch (priceLabel(c.rngDropPriceFormat)) {
            case "SHORT" -> "FULL";
            case "FULL" -> "COINS";
            default -> "SHORT";
        };
        c.save();
    }

    private String priceLabel(String value) {
        if (value == null) return "SHORT";
        return switch (value.toUpperCase(Locale.ROOT)) { case "FULL" -> "FULL"; case "COINS" -> "COINS"; default -> "SHORT"; };
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }
}
