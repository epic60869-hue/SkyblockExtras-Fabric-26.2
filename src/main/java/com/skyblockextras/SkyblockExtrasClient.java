package com.skyblockextras;

import com.skyblockextras.config.SbeConfig;
import com.skyblockextras.rng.RngTracker;
import com.skyblockextras.pet.PetOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public class SkyblockExtrasClient implements ClientModInitializer {
    public static final String MOD_ID = "skyblockextras";
    public static SbeConfig CONFIG;
    public static RngTracker RNG;
    public static PetOverlay PET;

    @Override
    public void onInitializeClient() {
        CONFIG = SbeConfig.load();
        RNG = new RngTracker(CONFIG);
        PET = new PetOverlay(CONFIG);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && Minecraft.getInstance().player != null) {
                RNG.handle(message);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PET.tick(client);
        });
    }
}
