package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag and mouse-wheel editor for the RNG announcement overlay. */
public class RngDropPositionEditorScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double dragX, dragY;

    public RngDropPositionEditorScreen(Screen parent) {
        super(Component.literal("RNG Drop Position Editor"));
        this.parent = parent;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(new PaintWidget(0, 0, width, height));
        addRenderableWidget(new BackButton(12, height - 38, 80, 25, "Back", () -> Minecraft.getInstance().gui.setScreen(parent)));
    }

    private int overlayWidth() { return 220; }
    private int overlayHeight() { return 72; }
    private int overlayX() { return SkyblockExtrasClient.CONFIG.rngDropOverlayX < 0 ? (width - overlayWidth()) / 2 : SkyblockExtrasClient.CONFIG.rngDropOverlayX; }
    private int overlayY() { return SkyblockExtrasClient.CONFIG.rngDropOverlayY < 0 ? (height - overlayHeight()) / 2 : SkyblockExtrasClient.CONFIG.rngDropOverlayY; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = overlayX(), y = overlayY();
        float scale = SkyblockExtrasClient.CONFIG.rngDropOverlayScale;
        g.fill(x, y, x + Math.round(220 * scale), y + Math.round(72 * scale), 0xE6101117);
        g.outline(x, y, Math.round(220 * scale), Math.round(72 * scale), 0xFFB96BFF);
        g.text(font, Component.literal("RNG DROP!"), x + 12, y + 12, 0xFFC77DFF, true);
        g.text(font, Component.literal("Fermento"), x + 12, y + 31, 0xFFFFFFFF, true);
        g.text(font, Component.literal("(2.45M coins)"), x + 12, y + 49, 0xFFFFD45A, true);
        g.text(font, Component.literal("Drag to move • Scroll to resize"), 10, 10, 0xFFE0E0E5, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int x = overlayX(), y = overlayY();
        int w = Math.round(220 * SkyblockExtrasClient.CONFIG.rngDropOverlayScale);
        int h = Math.round(72 * SkyblockExtrasClient.CONFIG.rngDropOverlayScale);
        if (e.x() >= x && e.x() <= x + w && e.y() >= y && e.y() <= y + h && e.button() == 0) {
            dragging = true; dragX = e.x() - x; dragY = e.y() - y; return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!dragging) return super.mouseDragged(e, dx, dy);
        int w = Math.round(220 * SkyblockExtrasClient.CONFIG.rngDropOverlayScale);
        int h = Math.round(72 * SkyblockExtrasClient.CONFIG.rngDropOverlayScale);
        SkyblockExtrasClient.CONFIG.rngDropOverlayX = Math.max(0, Math.min(width - w, (int)(e.x() - dragX)));
        SkyblockExtrasClient.CONFIG.rngDropOverlayY = Math.max(0, Math.min(height - h, (int)(e.y() - dragY)));
        SkyblockExtrasClient.CONFIG.save();
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        dragging = false;
        return super.mouseReleased(e);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int x = overlayX(), y = overlayY();
        int w = Math.round(220 * SkyblockExtrasClient.CONFIG.rngDropOverlayScale);
        int h = Math.round(72 * SkyblockExtrasClient.CONFIG.rngDropOverlayScale);
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            float scale = SkyblockExtrasClient.CONFIG.rngDropOverlayScale + (verticalAmount > 0 ? 0.1f : -0.1f);
            SkyblockExtrasClient.CONFIG.rngDropOverlayScale = Math.max(0.5f, Math.min(3.0f, scale));
            SkyblockExtrasClient.CONFIG.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static class PaintWidget extends AbstractWidget {
        PaintWidget(int x, int y, int w, int h) { super(x, y, w, h, Component.empty()); active = false; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) { g.fill(0, 0, width, height, 0x99070A0F); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }
    private static class BackButton extends AbstractWidget {
        private final Runnable action;
        BackButton(int x, int y, int w, int h, String text, Runnable action) { super(x, y, w, h, Component.literal(text)); this.action = action; }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) { g.fill(getX(), getY(), getX()+width, getY()+height, 0xFF5E386F); g.text(Minecraft.getInstance().font, getMessage(), getX()+22, getY()+8, 0xFFFFFFFF, false); }
        @Override public void onClick(MouseButtonEvent e, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput n) { }
    }
}
