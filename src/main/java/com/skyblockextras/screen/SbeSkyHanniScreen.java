package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Clean SkyHanni-style SBE configuration screen. */
public class SbeSkyHanniScreen extends Screen {
    private static final String[] CATEGORIES = {"About", "GUI", "Farming RNG", "Pet", "Discord Webhook"};
    private static final int CARD_H = 54;
    private static final int CARD_GAP = 8;

    private final Screen parent;
    private int selected = 0;
    private int left, top, panelW, panelH;
    private int sidebarLeft, sidebarTop, sidebarWidth, sidebarHeight;
    private int contentLeft, contentTop, contentRight, contentBottom;
    private EditBox webhookBox;

    public SbeSkyHanniScreen(Screen parent) {
        super(Component.literal("Skyblock Extras"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(1160, width - 32);
        panelH = Math.min(760, height - 32);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;

        sidebarLeft = left + 16;
        sidebarTop = top + 72;
        sidebarWidth = 210;
        sidebarHeight = panelH - 120;

        contentLeft = sidebarLeft + sidebarWidth + 18;
        contentTop = sidebarTop;
        contentRight = left + panelW - 16;
        contentBottom = top + panelH - 48;

        int inputX = contentLeft + 26;
        int inputY = contentTop + 172;
        int inputW = contentRight - contentLeft - 52;
        webhookBox = new EditBox(font, inputX, inputY, inputW, 28, Component.literal("Webhook URL"));
        webhookBox.setMaxLength(300);
        webhookBox.setValue(SkyblockExtrasClient.CONFIG == null ? "" : SkyblockExtrasClient.CONFIG.discordWebhookUrl);
        webhookBox.setVisible(selected == 4);
        addRenderableWidget(webhookBox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xB808090D);
        g.fill(left, top, left + panelW, top + panelH, 0xFF15161B);
        g.outline(left, top, left + panelW, top + panelH, 0xFF35363F);

        g.text(font, Component.literal("SKYBLOCK EXTRAS"), left + 24, top + 20, 0xFFF3F3F5, true);
        g.text(font, Component.literal("Configuration"), left + 24, top + 39, 0xFF888993, false);

        drawSidebar(g, mouseX, mouseY);
        g.fill(contentLeft, contentTop, contentRight, contentBottom, 0xFF0F1014);
        drawContent(g, mouseX, mouseY);

        int doneW = 90;
        int doneH = 28;
        int doneX = left + panelW - doneW - 18;
        int doneY = top + panelH - doneH - 12;
        drawButton(g, doneX, doneY, doneW, doneH, "Done", inside(mouseX, mouseY, doneX, doneY, doneW, doneH));

        // Screen owns the EditBox rendering/input lifecycle. This must be called
        // after our custom background/content extraction.
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(sidebarLeft, sidebarTop, sidebarLeft + sidebarWidth, sidebarTop + sidebarHeight, 0xFF101116);
        g.outline(sidebarLeft, sidebarTop, sidebarLeft + sidebarWidth, sidebarTop + sidebarHeight, 0xFF292A31);
        g.text(font, Component.literal("FEATURES"), sidebarLeft + 16, sidebarTop + 15, 0xFF777883, true);

        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = sidebarTop + 42 + i * 48;
            boolean active = selected == i;
            boolean hover = inside(mouseX, mouseY, sidebarLeft + 8, y, sidebarWidth - 16, 36);
            if (active) {
                g.fill(sidebarLeft + 8, y, sidebarLeft + sidebarWidth - 8, y + 36, 0xFF34223F);
                g.fill(sidebarLeft + 8, y, sidebarLeft + 11, y + 36, 0xFFC77DFF);
            } else if (hover) {
                g.fill(sidebarLeft + 8, y, sidebarLeft + sidebarWidth - 8, y + 36, 0xFF1D1E24);
            }
            g.text(font, Component.literal(CATEGORIES[i]), sidebarLeft + 20, y + 11, active ? 0xFFE1B9F3 : 0xFFD7D7DC, false);
        }
    }

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = contentLeft + 22;
        int y = contentTop + 18;
        String subtitle = switch (selected) {
            case 1 -> "HUD elements and visual positioning";
            case 2 -> "Persistent timers for rare farming drops";
            case 3 -> "Active pet display and overflow XP";
            case 4 -> "One live Discord message for the current RNG session";
            default -> "SkyBlock utilities and configuration";
        };

        g.text(font, Component.literal(CATEGORIES[selected]), x, y, 0xFFF0F0F4, true);
        g.text(font, Component.literal(subtitle), x, y + 20, 0xFF898A95, false);
        g.fill(x, y + 41, contentRight - 22, y + 42, 0xFF2A2B32);

        int rowY = y + 56;
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        if (c == null) return;

        switch (selected) {
            case 0 -> drawAbout(g, rowY);
            case 1 -> {
                drawRow(g, rowY, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Background", "Show a background behind the pet HUD.", c.petBackgroundEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "RNG Drop Overlay", "Show a large notification for tracked drops.", c.rngDropOverlayEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "RNG Background", "Show the RNG notification background.", c.rngDropOverlayBackgroundEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Icon", "Show the pet icon.", c.showPetIcon); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Level", "Show the pet and overflow level.", c.showPetLevel); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet XP", "Show total pet XP.", c.showPetXp); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Item", "Show the held pet item.", c.showPetItem);
            }
            case 2 -> {
                drawRow(g, rowY, "Farming RNG", "Master switch for farming RNG tracking.", c.farmingRngEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Feast Drops", "Track all Harvest Feast drops as one group.", c.harvestFeastEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Slugs", "Master switch for Slug tracking.", c.slugEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Epic Slug", "Track Epic Slug.", c.epicSlug); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Legendary Slug", "Track Legendary Slug.", c.legendarySlug); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Dyes", "Track configured farming dyes.", c.dyesEnabled);
            }
            case 3 -> {
                drawRow(g, rowY, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Background", "Toggle the HUD background.", c.petBackgroundEnabled); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Icon", "Show the actual pet icon when available.", c.showPetIcon); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Level", "Show the normal or overflow level.", c.showPetLevel); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Level Progress", "Show progress toward the next level.", c.showPetProgress); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet XP", "Show total pet XP.", c.showPetXp); rowY += CARD_H + CARD_GAP;
                drawRow(g, rowY, "Pet Item", "Show the held pet item.", c.showPetItem);
            }
            case 4 -> drawDiscord(g, rowY, mouseX, mouseY, c);
        }
    }

    private void drawAbout(GuiGraphicsExtractor g, int y) {
        drawInfo(g, y, "Skyblock Extras", "Client-side SkyBlock utilities for Minecraft 26.2.", "v0.1.2");
        drawInfo(g, y + 70, "Farming RNG", "Persistent rare-drop timers and center-screen drop alerts.", "RNG");
        drawInfo(g, y + 140, "Pet", "Active pet, progress, total XP, held item and overflow levels.", "PET");
        drawInfo(g, y + 210, "Discord", "Optional direct webhook session tracking for RNG drops.", "WEBHOOK");
    }

    private void drawDiscord(GuiGraphicsExtractor g, int y, int mouseX, int mouseY, SbeConfig c) {
        drawRow(g, y, "Discord Webhook", "Enable the live RNG session message.", c.discordWebhookEnabled);
        int boxY = y + 76;
        g.text(font, Component.literal("WEBHOOK URL"), contentLeft + 26, boxY, 0xFF777883, true);
        g.text(font, Component.literal("Paste your Discord webhook URL below, then press Save."), contentLeft + 26, boxY + 19, 0xFF898A95, false);

        if (webhookBox != null) {
            webhookBox.setVisible(true);
            // Keep the real widget at one stable position; changing its position
            // during extraction caused the oversized purple rectangles seen before.
            webhookBox.setPosition(contentLeft + 26, boxY + 38);
            webhookBox.setWidth(contentRight - contentLeft - 52);
        }

        int buttonY = boxY + 76;
        drawButton(g, contentLeft + 26, buttonY, 110, 28, "Save", inside(mouseX, mouseY, contentLeft + 26, buttonY, 110, 28));
        drawButton(g, contentLeft + 144, buttonY, 110, 28, "Test", inside(mouseX, mouseY, contentLeft + 144, buttonY, 110, 28));

        g.text(font, Component.literal("The same Discord message is edited as new RNG drops happen."), contentLeft + 26, buttonY + 49, 0xFF777881, false);
        g.text(font, Component.literal("Session uptime starts when Minecraft launches."), contentLeft + 26, buttonY + 66, 0xFF777881, false);
    }

    private void drawRow(GuiGraphicsExtractor g, int y, String title, String desc, boolean value) {
        int x1 = contentLeft + 16;
        int x2 = contentRight - 16;
        g.fill(x1, y, x2, y + CARD_H, 0xFF1D1E24);
        g.outline(x1, y, x2, y + CARD_H, 0xFF30313A);
        g.text(font, Component.literal(title), x1 + 14, y + 9, 0xFFE7E7EC, true);
        g.text(font, Component.literal(desc), x1 + 14, y + 29, 0xFF898A95, false);
        drawToggle(g, x2 - 52, y + 17, value);
    }

    private void drawInfo(GuiGraphicsExtractor g, int y, String title, String desc, String badge) {
        int x1 = contentLeft + 16;
        int x2 = contentRight - 16;
        g.fill(x1, y, x2, y + 62, 0xFF1D1E24);
        g.outline(x1, y, x2, y + 62, 0xFF30313A);
        g.text(font, Component.literal(title), x1 + 14, y + 12, 0xFFE7E7EC, true);
        g.text(font, Component.literal(desc), x1 + 14, y + 34, 0xFF92939D, false);
        g.fill(x2 - 82, y + 18, x2 - 14, y + 43, 0xFF292033);
        g.text(font, Component.literal(badge), x2 - 68, y + 26, 0xFFC77DFF, false);
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, boolean on) {
        g.fill(x, y, x + 38, y + 20, on ? 0xFF59376D : 0xFF292A30);
        g.outline(x, y, x + 38, y + 20, on ? 0xFF9962B7 : 0xFF45464F);
        if (on) g.fill(x + 21, y + 3, x + 35, y + 17, 0xFFE8D3F2);
        else g.fill(x + 3, y + 3, x + 17, y + 17, 0xFF85868E);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? 0xFF3D294A : 0xFF30243B);
        g.outline(x, y, x + w, y + h, 0xFF69477F);
        g.text(font, Component.literal(label), x + (w - font.width(label)) / 2, y + 10, 0xFFE8D9EF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = sidebarTop + 42 + i * 48;
            if (inside(mx, my, sidebarLeft + 8, y, sidebarWidth - 16, 36)) {
                selected = i;
                if (webhookBox != null) webhookBox.setVisible(selected == 4);
                return true;
            }
        }

        int rowY = contentTop + 74;
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        if (c != null) {
            if (selected == 1) {
                if (toggleHit(mx,my,rowY)) { c.petOverlayEnabled=!c.petOverlayEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.petBackgroundEnabled=!c.petBackgroundEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.rngDropOverlayEnabled=!c.rngDropOverlayEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.rngDropOverlayBackgroundEnabled=!c.rngDropOverlayBackgroundEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetIcon=!c.showPetIcon; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetLevel=!c.showPetLevel; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetXp=!c.showPetXp; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetItem=!c.showPetItem; save(); return true; }
            } else if (selected == 2) {
                if (toggleHit(mx,my,rowY)) { c.farmingRngEnabled=!c.farmingRngEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.harvestFeastEnabled=!c.harvestFeastEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.slugEnabled=!c.slugEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.epicSlug=!c.epicSlug; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.legendarySlug=!c.legendarySlug; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.dyesEnabled=!c.dyesEnabled; save(); return true; }
            } else if (selected == 3) {
                if (toggleHit(mx,my,rowY)) { c.petOverlayEnabled=!c.petOverlayEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.petBackgroundEnabled=!c.petBackgroundEnabled; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetIcon=!c.showPetIcon; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetLevel=!c.showPetLevel; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetProgress=!c.showPetProgress; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetXp=!c.showPetXp; save(); return true; } rowY += CARD_H + CARD_GAP;
                if (toggleHit(mx,my,rowY)) { c.showPetItem=!c.showPetItem; save(); return true; }
            } else if (selected == 4) {
                if (toggleHit(mx,my,rowY)) { c.discordWebhookEnabled=!c.discordWebhookEnabled; save(); return true; }
                int boxY = rowY + 76;
                int buttonY = boxY + 76;
                if (inside(mx,my,contentLeft+26,buttonY,110,28)) {
                    c.discordWebhookUrl = webhookBox == null ? "" : webhookBox.getValue().trim();
                    save();
                    if (SkyblockExtrasClient.DISCORD_WEBHOOK != null) SkyblockExtrasClient.DISCORD_WEBHOOK.resetForNewWebhook();
                    return true;
                }
                if (inside(mx,my,contentLeft+144,buttonY,110,28)) {
                    c.discordWebhookUrl = webhookBox == null ? "" : webhookBox.getValue().trim();
                    save();
                    if (SkyblockExtrasClient.DISCORD_WEBHOOK != null) SkyblockExtrasClient.DISCORD_WEBHOOK.test();
                    return true;
                }
            }
        }

        int doneX = left + panelW - 108;
        int doneY = top + panelH - 40;
        if (inside(mx,my,doneX,doneY,90,28)) { onClose(); return true; }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean toggleHit(double x, double y, int rowY) {
        return inside(x, y, contentLeft + 16, rowY, contentRight - contentLeft - 32, CARD_H);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private void save() { if (SkyblockExtrasClient.CONFIG != null) SkyblockExtrasClient.CONFIG.save(); }

    @Override
    public void onClose() {
        if (parent != null) Minecraft.getInstance().gui.setScreen(parent);
        else Minecraft.getInstance().gui.setScreen(null);
    }
}