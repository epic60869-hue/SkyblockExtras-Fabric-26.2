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

/** Drag-and-drop position/scale editor for the pet overlay. */
public class PositionEditorScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

    public PositionEditorScreen(Screen parent) {
        super(Component.literal("Skyblock Extras Position Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new EditorButton(14, 14, 70, 25, "BACK", this::onClose));
    }

    private int previewX() {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int previewW = previewWidth();
        return Math.max(0, Math.min(width - previewW, c.petX));
    }

    private int previewY() {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int previewH = previewHeight();
        return Math.max(0, Math.min(height - previewH, c.petY));
    }

    private int previewWidth() {
        return Math.max(190, Math.round(190 * SkyblockExtrasClient.CONFIG.petScale));
    }

    private int previewHeight() {
        return Math.max(72, Math.round(72 * SkyblockExtrasClient.CONFIG.petScale));
    }

    private void clampPosition() {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        int maxX = Math.max(0, width - previewWidth());
        int maxY = Math.max(0, height - previewHeight());
        c.petX = Math.max(0, Math.min(maxX, c.petX));
        c.petY = Math.max(0, Math.min(maxY, c.petY));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int px = previewX();
            int py = previewY();
            int pw = previewWidth();
            int ph = previewHeight();

            if (event.x() >= px && event.x() <= px + pw
                    && event.y() >= py && event.y() <= py + ph) {
                dragging = true;
                dragOffsetX = event.x() - px;
                dragOffsetY = event.y() - py;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            SbeConfig c = SkyblockExtrasClient.CONFIG;
            int newX = (int) Math.round(event.x() - dragOffsetX);
            int newY = (int) Math.round(event.y() - dragOffsetY);

            int maxX = Math.max(0, width - previewWidth());
            int maxY = Math.max(0, height - previewHeight());

            c.petX = Math.max(0, Math.min(maxX, newX));
            c.petY = Math.max(0, Math.min(maxY, newY));
            c.save();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            SkyblockExtrasClient.CONFIG.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int px = previewX();
        int py = previewY();
        int pw = previewWidth();
        int ph = previewHeight();

        if (mouseX >= px && mouseX <= px + pw
                && mouseY >= py && mouseY <= py + ph
                && verticalAmount != 0.0) {
            SbeConfig c = SkyblockExtrasClient.CONFIG;
            float oldScale = c.petScale;
            float step = verticalAmount > 0.0 ? 0.10f : -0.10f;
            float newScale = Math.max(0.50f, Math.min(3.00f, oldScale + step));

            if (newScale != oldScale) {
                c.petScale = newScale;
                clampPosition();
                c.save();
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        SbeConfig c = SkyblockExtrasClient.CONFIG;

        g.fill(0, 0, width, height, 0xAA08090D);
        g.fill(12, 12, 190, 40, 0xFF1E1F25);
        g.outline(12, 12, 178, 28, 0xFF44454E);
        g.text(font, "POSITION EDITOR", 22, 22, 0xFFE7E7EC, false);

        g.text(font, "Drag the Pet Overlay to move it", 20, 56, 0xFFB4B5BE, false);
        g.text(font, "Scroll over the overlay to resize it", 20, 73, 0xFF8F909A, false);
        g.text(font, "X: " + c.petX + "   Y: " + c.petY + "   Scale: " + String.format("%.2f", c.petScale),
                20, 90, 0xFFC276FF, false);

        int previewX = previewX();
        int previewY = previewY();
        int previewW = previewWidth();
        int previewH = previewHeight();

        boolean hovered = mouseX >= previewX && mouseX <= previewX + previewW
                && mouseY >= previewY && mouseY <= previewY + previewH;

        int border = dragging ? 0xFFE0A7FF : (hovered ? 0xFFC276FF : 0xFF8A42D2);
        g.fill(previewX, previewY, previewX + previewW, previewY + previewH,
                dragging ? 0xFF2A2030 : 0xFF202126);
        g.outline(previewX, previewY, previewW, previewH, border);
        g.text(font, "PET OVERLAY", previewX + 12, previewY + 12, 0xFFE8D5F5, false);
        g.text(font, "[Lvl 100] Pet Name", previewX + 12, previewY + 30, 0xFFD5D5DC, false);
        g.text(font, "XP  12.3M / 20.0M", previewX + 12, previewY + 48, 0xFF9B9CA8, false);
        g.text(font, dragging ? "Dragging..." : (hovered ? "Scroll to resize" : "Click and drag me"),
                previewX + 12, previewY + 63, dragging ? 0xFFC276FF : 0xFF777984, false);

        g.text(font, "Drag to position • Scroll to resize • BACK to return",
                20, height - 28, 0xFF8F909A, false);
    }

    private class EditorButton extends AbstractWidget {
        private final Runnable action;

        EditorButton(int x, int y, int w, int h, String text, Runnable action) {
            super(x, y, w, h, Component.literal(text));
            this.action = action;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            boolean hover = isHovered();
            g.fill(getX(), getY(), getX() + width, getY() + height,
                    hover ? 0xFF292A31 : 0xFF222329);
            g.outline(getX(), getY(), width, height,
                    hover ? 0xFFB86AF0 : 0xFF4A4B54);
            int tw = PositionEditorScreen.this.font.width(getMessage());
            g.text(PositionEditorScreen.this.font, getMessage(),
                    getX() + (width - tw) / 2, getY() + 8, 0xFFE0E0E5, false);
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
