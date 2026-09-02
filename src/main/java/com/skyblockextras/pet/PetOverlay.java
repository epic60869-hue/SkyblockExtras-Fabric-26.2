package com.skyblockextras.pet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class PetOverlay {

    private boolean enabled = true;

    public void tick(Minecraft client) {
        // Pet data handling will be implemented here.
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!enabled) {
            return;
        }

        // Temporary rendering placeholder.
        // The actual pet overlay will be implemented here.
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
