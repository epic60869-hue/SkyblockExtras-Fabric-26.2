package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** SkyHanni-inspired SBE configuration screen. */
public class SbeSkyHanniScreen extends Screen {
    private static final String[] CATEGORIES = {"About", "GUI", "Farming RNG", "Pet", "Discord Webhook"};
    private final Screen parent;
    private int selected;
    private int left, top, panelW, panelH;
    private int sidebarLeft, sidebarTop, sidebarWidth;
    private int contentLeft, contentTop, contentRight, contentBottom;
    private EditBox webhookBox;

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
        webhookBox = new EditBox(font, contentLeft + 30, contentTop + 130, contentRight - contentLeft - 60, 24, Component.literal("Discord Webhook URL"));
        webhookBox.setMaxLength(300);
        webhookBox.setValue(SkyblockExtrasClient.CONFIG == null ? "" : SkyblockExtrasClient.CONFIG.discordWebhookUrl);
        webhookBox.setVisible(selected == 4);
        addRenderableWidget(webhookBox);
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
        drawContent(g, mouseX, mouseY);
        int doneX = left + panelW - 106, doneY = top + panelH - 40;
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

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = contentTop + 18;
        String subtitle = switch (selected) {
            case 1 -> "HUD elements and visual positioning";
            case 2 -> "Persistent timers for rare farming drops";
            case 3 -> "Active pet display and overflow XP";
            case 4 -> "Send one live RNG session message to Discord";
            default -> "Skyblock Extras configuration";
        };
        text(g, CATEGORIES[selected], contentLeft + 20, y, 0xFFF0F0F4, true);
        text(g, subtitle, contentLeft + 20, y + 20, 0xFF898A95, false);
        g.fill(contentLeft + 18, y + 42, contentRight - 18, y + 43, 0xFF2B2C34);
        y += 58;
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        if (selected == 0) {
            info(g, y, "Skyblock Extras", "Client-side SkyBlock utilities for Minecraft 26.2.", "v0.1.2");
            info(g, y + 104, "GUI controls", "Use the feature pages to configure overlays and tracking.", "READY");
            info(g, y + 208, "Discord", "Optional direct webhook session tracking for RNG drops.", "WEBHOOK");
            return;
        }
        if (selected == 1) {
            row(g, y, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled); y += 56;
            row(g, y, "Pet Background", "Show the pet HUD background.", c.petBackgroundEnabled); y += 56;
            row(g, y, "RNG Drop Overlay", "Show a notification when a tracked RNG drop occurs.", c.rngDropOverlayEnabled); y += 56;
            row(g, y, "RNG Background", "Show the RNG announcement background.", c.rngDropOverlayBackgroundEnabled); y += 56;
            row(g, y, "Pet Icon", "Show the pet icon.", c.showPetIcon); y += 56;
            row(g, y, "Pet Level", "Show the pet and overflow level.", c.showPetLevel); y += 56;
            row(g, y, "Pet XP", "Show total pet XP.", c.showPetXp); y += 56;
            row(g, y, "Pet Item", "Show the held pet item.", c.showPetItem);
            return;
        }
        if (selected == 2) {
            row(g, y, "Farming RNG", "Master switch for farming RNG tracking.", c.farmingRngEnabled); y += 56;
            row(g, y, "Feast Drops", "Track all 17 Harvest Feast drops as one group.", c.harvestFeastEnabled); y += 56;
            row(g, y, "Slugs", "Master switch for Slug drops.", c.slugEnabled); y += 56;
            row(g, y, "Epic Slug", "Track Epic Slug.", c.epicSlug); y += 56;
            row(g, y, "Legendary Slug", "Track Legendary Slug.", c.legendarySlug); y += 56;
            row(g, y, "Dyes", "Track configured farming-related dyes.", c.dyesEnabled);
            return;
        }
        if (selected == 3) {
            row(g, y, "Pet Overlay", "Display the currently equipped pet.", c.petOverlayEnabled); y += 56;
            row(g, y, "Pet Background", "Toggle the HUD background.", c.petBackgroundEnabled); y += 56;
            row(g, y, "Pet Icon", "Show the actual pet icon when available.", c.showPetIcon); y += 56;
            row(g, y, "Pet Level", "Show the combined normal/overflow level.", c.showPetLevel); y += 56;
            row(g, y, "Level Progress", "Show progress toward the next level/overflow level.", c.showPetProgress); y += 56;
            row(g, y, "Pet XP", "Show total pet XP.", c.showPetXp); y += 56;
            row(g, y, "Pet Item", "Show the held pet item.", c.showPetItem);
            return;
        }
        row(g, y, "Discord Webhook", "Enable the live RNG session message.", c.discordWebhookEnabled); y += 58;
        text(g, "WEBHOOK URL", contentLeft + 30, y + 3, 0xFF777883, true);
        text(g, "The URL is saved locally in your SBE config.", contentLeft + 30, y + 20, 0xFF898A95, false);
        if (webhookBox != null) {
            webhookBox.setPosition(contentLeft + 30, y + 36);
            webhookBox.setVisible(true);
        }
        button(g, contentLeft + 30, y + 68, 100, 27, "Save", mouseX >= contentLeft + 30 && mouseX <= contentLeft + 130 && mouseY >= y + 68 && mouseY <= y + 95);
        button(g, contentLeft + 140, y + 68, 100, 27, "Test", mouseX >= contentLeft + 140 && mouseX <= contentLeft + 240 && mouseY >= y + 68 && mouseY <= y + 95);
        text(g, "The message updates instead of sending a new Discord message for every drop.", contentLeft + 30, y + 116, 0xFF777881, false);
        text(g, "Session uptime starts when Minecraft launches and resets on restart.", contentLeft + 30, y + 134, 0xFF777881, false);
    }

    private void row(GuiGraphicsExtractor g, int y, String title, String desc, boolean value) {
        card(g, y, 48);
        text(g, title, contentLeft + 30, y + 8, 0xFFE5E5EA, true);
        text(g, desc, contentLeft + 30, y + 26, 0xFF898A95, false);
        toggle(g, contentRight - 62, y + 13, value);
    }

    private void info(GuiGraphicsExtractor g, int y, String title, String desc, String badge) {
        card(g, y, 92);
        text(g, title, contentLeft + 30, y + 17, 0xFFE7E7EC, true);
        text(g, desc, contentLeft + 30, y + 42, 0xFF92939D, false);
        g.fill(contentRight - 94, y + 14, contentRight - 14, y + 39, 0xFF27202F);
        text(g, badge, contentRight - 79, y + 21, 0xFFC77DFF, false);
    }

    private void card(GuiGraphicsExtractor g, int y, int h) {
        g.fill(contentLeft + 16, y, contentRight - 16, y + h, 0xFF1E1F25);
        g.outline(contentLeft + 16, y, contentRight - 16, y + h, 0xFF30313A);
    }

    private void toggle(GuiGraphicsExtractor g, int x, int y, boolean on) {
        g.fill(x, y, x + 38, y + 20, on ? 0xFF57376C : 0xFF292A30);
        g.outline(x, y, x + 38, y + 20, on ? 0xFF8C58A9 : 0xFF45464F);
        g.fill(on ? x + 21 : x + 3, y + 3, on ? x + 35 : x + 17, y + 17, on ? 0xFFE1C9EC : 0xFF85868E);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? 0xFF3A2947 : 0xFF30243B);
        g.outline(x, y, x + w, y + h, 0xFF69477F);
        g.text(font, Component.literal(label), x + (w - font.width(label)) / 2, y + (h - 9) / 2, 0xFFE5D5ED, false);
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color, boolean shadow) {
        g.text(font, Component.literal(value), x, y, color, shadow);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = sidebarTop + 40 + i * 45;
            if (mx >= sidebarLeft + 7 && mx <= sidebarLeft + sidebarWidth - 7 && my >= y && my <= y + 34) {
                selected = i;
                if (webhookBox != null) webhookBox.setVisible(selected == 4);
                return true;
            }
        }
        int y = contentTop + 76;
        if (selected == 1) {
            if (hit(mx,my,y)) { c().petOverlayEnabled=!c().petOverlayEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().petBackgroundEnabled=!c().petBackgroundEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().rngDropOverlayEnabled=!c().rngDropOverlayEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().rngDropOverlayBackgroundEnabled=!c().rngDropOverlayBackgroundEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetIcon=!c().showPetIcon; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetLevel=!c().showPetLevel; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetXp=!c().showPetXp; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetItem=!c().showPetItem; save(); return true; }
        } else if (selected == 2) {
            if (hit(mx,my,y)) { c().farmingRngEnabled=!c().farmingRngEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().harvestFeastEnabled=!c().harvestFeastEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().slugEnabled=!c().slugEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().epicSlug=!c().epicSlug; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().legendarySlug=!c().legendarySlug; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().dyesEnabled=!c().dyesEnabled; save(); return true; }
        } else if (selected == 3) {
            if (hit(mx,my,y)) { c().petOverlayEnabled=!c().petOverlayEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().petBackgroundEnabled=!c().petBackgroundEnabled; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetIcon=!c().showPetIcon; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetLevel=!c().showPetLevel; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetProgress=!c().showPetProgress; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetXp=!c().showPetXp; save(); return true; } y+=56;
            if (hit(mx,my,y)) { c().showPetItem=!c().showPetItem; save(); return true; }
        } else if (selected == 4) {
            if (hit(mx,my,y)) { c().discordWebhookEnabled=!c().discordWebhookEnabled; save(); return true; }
            y += 58;
            int saveY = y + 68;
            if (mx >= contentLeft+30 && mx <= contentLeft+130 && my >= saveY && my <= saveY+27) {
                c().discordWebhookUrl = webhookBox.getValue().trim(); save();
                if (SkyblockExtrasClient.DISCORD_WEBHOOK != null) SkyblockExtrasClient.DISCORD_WEBHOOK.resetForNewWebhook();
                return true;
            }
            if (mx >= contentLeft+140 && mx <= contentLeft+240 && my >= saveY && my <= saveY+27) {
                c().discordWebhookUrl = webhookBox.getValue().trim(); save();
                if (SkyblockExtrasClient.DISCORD_WEBHOOK != null) SkyblockExtrasClient.DISCORD_WEBHOOK.test();
                return true;
            }
        }
        int doneX=left+panelW-106, doneY=top+panelH-40;
        if (mx>=doneX && mx<=doneX+88 && my>=doneY && my<=doneY+26) { onClose(); return true; }
        return super.mouseClicked(event, doubleClick);
    }

    private SbeConfig c() { return SkyblockExtrasClient.CONFIG; }
    private boolean hit(double x,double y,int rowY) { return x>=contentLeft+16 && x<=contentRight-16 && y>=rowY && y<=rowY+48; }
    private void save() { if (c()!=null) c().save(); }

    @Override
    public void onClose() {
        if (parent != null) Minecraft.getInstance().gui.setScreen(parent);
        else Minecraft.getInstance().gui.setScreen(null);
    }
}