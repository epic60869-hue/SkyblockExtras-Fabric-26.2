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

/** Simple position/scale editor for the pet overlay. */
public class PositionEditorScreen extends Screen {
    private final Screen parent;

    public PositionEditorScreen(Screen parent) {
        super(Component.literal("Skyblock Extras Position Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int y = height - 42;
        addRenderableWidget(new EditorButton(width / 2 - 150, y, 70, 25, "X -", () -> changeX(-5)));
        addRenderableWidget(new EditorButton(width / 2 - 75, y, 70, 25, "X +", () -> changeX(5)));
        addRenderableWidget(new EditorButton(width / 2, y, 70, 25, "Y -", () -> changeY(-5)));
        addRenderableWidget(new EditorButton(width / 2 + 75, y, 70, 25, "Y +", () -> changeY(5)));
        addRenderableWidget(new EditorButton(width / 2 + 150, y, 90, 25, "SCALE +", () -> changeScale(0.05f)));
        addRenderableWidget(new EditorButton(width / 2 - 240, y, 80, 25, "SCALE -", () -> changeScale(-0.05f)));
        addRenderableWidget(new EditorButton(14, 14, 70, 25, "BACK", this::onClose));
    }

    private void changeX(int amount) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        c.petX += amount;
        c.save();
    }

    private void changeY(int amount) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        c.petY += amount;
        c.save();
    }

    private void changeScale(float amount) {
        SbeConfig c = SkyblockExtrasClient.CONFIG;
        c.petScale = Math.max(0.5f, Math.min(3.0f, c.petScale + amount));
        c.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        SbeConfig c = SkyblockExtrasClient.CONFIG;

        g.fill(0, 0, width, height, 0xAA08090D);
        g.fill(12, 12, 190, 40, 0xFF1E1F25);
        g.outline(12, 12, 178, 28, 0xFF44454E);
        g.text(font, "POSITION EDITOR", 22, 22, 0xFFE7E7EC, false);

        g.text(font, "Drag/position controls for the Pet Overlay", 20, 56, 0xFFB4B5BE, false);
        g.text(font, "X: " + c.petX + "   Y: " + c.petY + "   Scale: " + String.format("%.2f", c.petScale),
                20, 72, 0xFFC276FF, false);

        int previewX = Math.max(10, Math.min(width - 230, c.petX));
        int previewY = Math.max(95, Math.min(height - 125, c.petY + 95));
        int previewW = Math.max(190, Math.round(190 * c.petScale));
        int previewH = Math.max(72, Math.round(72 * c.petScale));
        g.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF202126);
        g.outline(previewX, previewY, previewW, previewH, 0xFFC276FF);
        g.text(font, "PET OVERLAY", previewX + 12, previewY + 12, 0xFFE8D5F5, false);
        g.text(font, "[Lvl 100] Pet Name", previewX + 12, previewY + 30, 0xFFD5D5DC, false);
        g.text(font, "XP  12.3M / 20.0M", previewX + 12, previewY + 48, 0xFF9B9CA8, false);

        g.text(font, "Use the controls below to move and resize the preview.",
                20, height - 68, 0xFF8F909A, false);
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
            g.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF292A31 : 0xFF222329);
            g.outline(getX(), getY(), width, height, hover ? 0xFFB86AF0 : 0xFF4A4B54);
            int tw = font.width(getMessage());
            g.text(font, getMessage(), getX() + (width - tw) / 2, getY() + 8, 0xFFE0E0E5, false);
        }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput b) { defaultButtonNarrationText(b); }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
