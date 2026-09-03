package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Compact SkyHanni/MoulConfig-inspired settings layout using SBE's existing config storage. */
public class SbeSkyHanniScreen extends Screen {
    private static final String[] CATEGORIES = {"About", "GUI", "Farming RNG", "Pet"};
    private static final String[] HARVEST = {
            "Aggourdian", "Botroot", "Cactus Flower", "Cane Knot", "Carrot Zest", "Cornucopia", "Cropie",
            "Crystalized Moonlight", "Deepfries", "Designer Coffee Beans", "Feastfungus", "Fermento",
            "Floral Gelatin", "Helianthus", "Melon Juice", "Salted Sunflower Seeds", "Squash"
    };
    private final Screen parent;
    private int selected;
    private int left, top, panelW, panelH, contentLeft, contentTop, contentRight, contentBottom;
    private int scroll, maxScroll;

    public SbeSkyHanniScreen(Screen parent) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
    }

    @Override protected void init() {
        panelW = Math.min(1000, width - 28); panelH = Math.min(690, height - 28);
        left = (width - panelW) / 2; top = (height - panelH) / 2;
        contentLeft = left + 230; contentTop = top + 70; contentRight = left + panelW - 16; contentBottom = top + panelH - 52;
        selected = Math.max(0, Math.min(selected, CATEGORIES.length - 1)); scroll = 0; recalc();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xB5090A0E);
        g.fill(left, top, left + panelW, top + panelH, 0xFF17171D); g.outline(left, top, left + panelW, top + panelH, 0xFF303039);
        g.text(font, Component.literal("SKYBLOCK EXTRAS"), left + 18, top + 15, 0xFFF1F1F4, true);
        g.text(font, Component.literal("Configuration"), left + 18, top + 31, 0xFF898A95, false);

        int sx = left + 12, sy = top + 56, sw = 202, sh = panelH - 108;
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF111217); g.outline(sx, sy, sx + sw, sy + sh, 0xFF292A31);
        g.text(font, Component.literal("FEATURES"), sx + 13, sy + 13, 0xFF747580, true);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = sy + 35 + i * 43; boolean active = selected == i;
            if (active) g.fill(sx + 7, y - 5, sx + sw - 7, y + 29, 0xFF292032);
            g.text(font, Component.literal(CATEGORIES[i]), sx + 18, y + 5, active ? 0xFFC77DFF : 0xFFD7D7DD, false);
        }

        g.fill(contentLeft - 1, contentTop - 1, contentRight + 1, contentBottom + 1, 0xFF0E0F13);
        g.fill(contentLeft, contentTop, contentRight, contentBottom, 0xFF17181D);
        drawContent(g);
        if (maxScroll > 0) {
            int tx = contentRight - 5, tt = contentTop + 8, tb = contentBottom - 8;
            g.fill(tx, tt, tx + 3, tb, 0xFF25262D);
            int th = Math.max(22, (tb - tt) * (contentBottom - contentTop) / Math.max(contentBottom - contentTop, contentHeight()));
            int travel = tb - tt - th; int ty = tt + Math.round(travel * (scroll / (float) maxScroll));
            g.fill(tx, ty, tx + 3, ty + th, 0xFF77707F);
        }
        int dx = left + panelW - 102, dy = top + panelH - 37;
        g.fill(dx, dy, dx + 86, dy + 24, 0xFF30243B); g.outline(dx, dy, dx + 86, dy + 24, 0xFF69477F);
        g.text(font, Component.literal("Done"), dx + 31, dy + 7, 0xFFE5D5ED, false);
    }

    private void drawContent(GuiGraphicsExtractor g) {
        int y = contentTop + 16 - scroll;
        String subtitle = switch (selected) { case 1 -> "HUD elements and visual positioning"; case 2 -> "Persistent timers for rare farming drops"; case 3 -> "Active pet display and overflow XP"; default -> "Skyblock Extras configuration"; };
        text(g, CATEGORIES[selected], contentLeft + 18, y, 0xFFF0F0F4, true); text(g, subtitle, contentLeft + 18, y + 20, 0xFF898A95, false);
        g.fill(contentLeft + 16, y + 42, contentRight - 16, y + 43, 0xFF2B2C34); y += 58;
        if (selected == 0) {
            info(g, y, "Skyblock Extras", "Client-side SkyBlock utilities for Minecraft 26.2.", "v0.1.2");
            info(g, y + 105, "SkyHanni-style GUI", "Compact categories, grouped options and scrollable settings.", "GUI");
            info(g, y + 210, "Pet + Farming", "Skysoft-inspired active pet HUD and Nopo-style RNG timers.", "READY"); return;
        }
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        if (selected == 1) {
            y = row(g, y, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled);
            y = row(g, y, "Pet Background", "Show the background behind the pet display.", c.petBackgroundEnabled);
            y = row(g, y, "RNG Drop Overlay", "Show a notification for tracked RNG drops.", c.rngDropOverlayEnabled);
            y = row(g, y, "RNG Background", "Show the background behind RNG announcements.", c.rngDropOverlayBackgroundEnabled);
            button(g, y, "Pet Position & Scale", "Open the drag/scale editor.", "EDIT"); y += 60;
            button(g, y, "RNG Position & Scale", "Open the drag/scale editor.", "EDIT"); y += 60;
            rowValue(g, y, "RNG Price Formatting", "SHORT / FULL / COINS", priceLabel(c.rngDropPriceFormat)); return;
        }
        if (selected == 2) {
            y = row(g, y, "Farming RNG", "Master switch for farming RNG tracking.", c.farmingRngEnabled);
            y = row(g, y, "Harvest Feast", "Track only the configured Feast drops.", c.harvestFeastEnabled);
            text(g, "HARVEST FEAST DROPS", contentLeft + 22, y + 2, 0xFF777883, true); y += 22;
            int gridW = contentRight - contentLeft - 44, colW = (gridW - 8) / 2;
            for (int i = 0; i < HARVEST.length; i++) { int col = i % 2, rr = i / 2; boolean on = c.harvestFeastDrops.getOrDefault(HARVEST[i], true); pill(g, contentLeft + 22 + col * (colW + 8), y + rr * 28, colW, HARVEST[i], on); }
            y += ((HARVEST.length + 1) / 2) * 28 + 10;
            y = row(g, y, "Slugs", "Track Epic and Legendary Slug drops.", c.slugEnabled);
            y = row(g, y, "Epic Slug", "Track Epic Slug.", c.epicSlug);
            y = row(g, y, "Legendary Slug", "Track Legendary Slug.", c.legendarySlug);
            row(g, y, "Dyes", "Track configured farming-related dyes.", c.dyesEnabled); return;
        }
        y = row(g, y, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled);
        y = row(g, y, "Pet Background", "Toggle the HUD background.", c.petBackgroundEnabled);
        y = row(g, y, "Pet Icon", "Show the pet icon.", c.showPetIcon);
        y = row(g, y, "Pet Level", "Show the combined level, including overflow.", c.showPetLevel);
        y = row(g, y, "Level Progress", "Show progress to the next normal level.", c.showPetProgress);
        y = row(g, y, "Pet XP", "Show total pet XP.", c.showPetXp);
        row(g, y, "Pet Item", "Show the currently equipped pet item.", c.showPetItem);
    }

    private int row(GuiGraphicsExtractor g, int y, String title, String desc, boolean value) { card(g, y, 48); text(g, title, contentLeft + 29, y + 8, 0xFFE5E5EA, true); text(g, desc, contentLeft + 29, y + 26, 0xFF898A95, false); toggle(g, contentRight - 67, y + 13, value); return y + 56; }
    private void rowValue(GuiGraphicsExtractor g, int y, String title, String desc, String value) { card(g, y, 52); text(g, title, contentLeft + 29, y + 8, 0xFFE5E5EA, true); text(g, desc, contentLeft + 29, y + 27, 0xFF898A95, false); text(g, value, contentRight - 72, y + 19, 0xFFC77DFF, false); }
    private void button(GuiGraphicsExtractor g, int y, String title, String desc, String label) { card(g, y, 52); text(g, title, contentLeft + 29, y + 8, 0xFFE5E5EA, true); text(g, desc, contentLeft + 29, y + 27, 0xFF898A95, false); g.fill(contentRight - 70, y + 12, contentRight - 12, y + 38, 0xFF30243B); g.outline(contentRight - 70, y + 12, contentRight - 12, y + 38, 0xFF69477F); text(g, label, contentRight - 55, y + 20, 0xFFE5D5ED, false); }
    private void info(GuiGraphicsExtractor g, int y, String title, String desc, String badge) { card(g, y, 92); text(g, title, contentLeft + 31, y + 17, 0xFFE7E7EC, true); text(g, desc, contentLeft + 31, y + 42, 0xFF92939D, false); g.fill(contentRight - 93, y + 14, contentRight - 12, y + 38, 0xFF27202F); text(g, badge, contentRight - 78, y + 21, 0xFFC77DFF, false); }
    private void card(GuiGraphicsExtractor g, int y, int h) { if (y + h < contentTop || y > contentBottom) return; g.fill(contentLeft + 16, y, contentRight - 16, y + h, 0xFF1E1F25); g.outline(contentLeft + 16, y, contentRight - 16, y + h, 0xFF30313A); }
    private void pill(GuiGraphicsExtractor g, int x, int y, int w, String name, boolean on) { if (y + 22 < contentTop || y > contentBottom) return; g.fill(x, y, x + w, y + 22, 0xFF1D1E23); g.outline(x, y, x + w, y + 22, on ? 0xFF68477B : 0xFF2D2E35); text(g, name, x + 8, y + 7, on ? 0xFFDCC5E8 : 0xFF777881, false); g.fill(x + w - 17, y + 6, x + w - 8, y + 15, on ? 0xFFC77DFF : 0xFF55565E); }
    private void toggle(GuiGraphicsExtractor g, int x, int y, boolean on) { g.fill(x, y, x + 38, y + 20, on ? 0xFF57376C : 0xFF292A30); g.outline(x, y, x + 38, y + 20, on ? 0xFF8C58A9 : 0xFF45464F); g.fill(on ? x + 21 : x + 3, y + 3, on ? x + 35 : x + 17, y + 17, on ? 0xFFE1C9EC : 0xFF85868E); }
    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color, boolean shadow) { if (y >= contentTop - 12 && y <= contentBottom + 4) g.text(font, Component.literal(s), x, y, color, shadow); }
    private String priceLabel(String f) { if (f == null) return "SHORT"; return switch (f.toUpperCase(Locale.ROOT)) { case "FULL" -> "FULL"; case "COINS" -> "COINS"; default -> "SHORT"; }; }
    private int contentHeight() { return switch (selected) { case 1 -> 58 + 6 * 56 + 60; case 2 -> 58 + 2 * 56 + 22 + ((HARVEST.length + 1) / 2) * 28 + 10 + 4 * 56; case 3 -> 58 + 7 * 56; default -> 58 + 3 * 105; }; }
    private void recalc() { maxScroll = Math.max(0, contentHeight() - (contentBottom - contentTop)); scroll = Math.max(0, Math.min(scroll, maxScroll)); }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() != 0) return super.mouseClicked(e, doubleClick); double mx = e.x(), my = e.y();
        int sx = left + 12, sy = top + 56;
        for (int i = 0; i < CATEGORIES.length; i++) { int y = sy + 30 + i * 43; if (mx >= sx && mx <= sx + 202 && my >= y && my <= y + 38) { selected = i; scroll = 0; recalc(); return true; } }
        int dx = left + panelW - 102, dy = top + panelH - 37; if (mx >= dx && mx <= dx + 86 && my >= dy && my <= dy + 24) { onClose(); return true; }
        if (mx < contentLeft || mx > contentRight || my < contentTop || my > contentBottom) return false;
        double cy = my + scroll; SbeConfig c = SkyblockExtrasClient.CONFIG;
        if (selected == 1) {
            if (hit(contentTop + 58, cy, 48)) { c.petOverlayEnabled = !c.petOverlayEnabled; c.save(); return true; }
            if (hit(contentTop + 114, cy, 48)) { c.petBackgroundEnabled = !c.petBackgroundEnabled; c.save(); return true; }
            if (hit(contentTop + 170, cy, 48)) { c.rngDropOverlayEnabled = !c.rngDropOverlayEnabled; c.save(); return true; }
            if (hit(contentTop + 226, cy, 48)) { c.rngDropOverlayBackgroundEnabled = !c.rngDropOverlayBackgroundEnabled; c.save(); return true; }
            if (hit(contentTop + 286, cy, 52)) { Minecraft.getInstance().gui.setScreen(new PositionEditorScreen(this)); return true; }
            if (hit(contentTop + 346, cy, 52)) { Minecraft.getInstance().gui.setScreen(new RngDropPositionEditorScreen(this)); return true; }
            if (hit(contentTop + 406, cy, 52)) { cyclePrice(c); return true; }
        } else if (selected == 2) {
            if (hit(contentTop + 58, cy, 48)) { c.farmingRngEnabled = !c.farmingRngEnabled; c.save(); return true; }
            if (hit(contentTop + 114, cy, 48)) { c.harvestFeastEnabled = !c.harvestFeastEnabled; c.save(); return true; }
            int gridTop = contentTop + 192, gridW = contentRight - contentLeft - 44, colW = (gridW - 8) / 2;
            for (int i = 0; i < HARVEST.length; i++) { int col = i % 2, rr = i / 2, xx = contentLeft + 22 + col * (colW + 8), yy = gridTop + rr * 28; if (mx >= xx && mx <= xx + colW && cy >= yy && cy <= yy + 22) { c.harvestFeastDrops.put(HARVEST[i], !c.harvestFeastDrops.getOrDefault(HARVEST[i], true)); c.save(); return true; } }
            int after = gridTop + ((HARVEST.length + 1) / 2) * 28 + 10;
            if (hit(after, cy, 48)) { c.slugEnabled = !c.slugEnabled; c.save(); return true; }
            if (hit(after + 56, cy, 48)) { c.epicSlug = !c.epicSlug; c.save(); return true; }
            if (hit(after + 112, cy, 48)) { c.legendarySlug = !c.legendarySlug; c.save(); return true; }
            if (hit(after + 168, cy, 48)) { c.dyesEnabled = !c.dyesEnabled; c.save(); return true; }
        } else if (selected == 3) {
            int b = contentTop + 58; boolean[] vals = {c.petOverlayEnabled,c.petBackgroundEnabled,c.showPetIcon,c.showPetLevel,c.showPetProgress,c.showPetXp,c.showPetItem};
            for (int i=0;i<vals.length;i++) if (hit(b+i*56, cy, 48)) { switch(i){case 0->c.petOverlayEnabled=!c.petOverlayEnabled;case 1->c.petBackgroundEnabled=!c.petBackgroundEnabled;case 2->c.showPetIcon=!c.showPetIcon;case 3->c.showPetLevel=!c.showPetLevel;case 4->c.showPetProgress=!c.showPetProgress;case 5->c.showPetXp=!c.showPetXp;case 6->c.showPetItem=!c.showPetItem;} c.save(); return true; }
        }
        return false;
    }
    private boolean hit(double y,double my,double h){return my>=y&&my<=y+h;}
    private void cyclePrice(SbeConfig c){String f=priceLabel(c.rngDropPriceFormat);c.rngDropPriceFormat=switch(f){case "SHORT"->"FULL";case "FULL"->"COINS";default->"SHORT";};c.save();}
    @Override public boolean mouseScrolled(double x,double y,double hx,double vy){if(x>=contentLeft&&x<=contentRight&&y>=contentTop&&y<=contentBottom&&maxScroll>0){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)Math.round(vy*28.0)));return true;}return super.mouseScrolled(x,y,hx,vy);}
    @Override public void onClose(){if(minecraft!=null)minecraft.gui.setScreen(parent);}
}
