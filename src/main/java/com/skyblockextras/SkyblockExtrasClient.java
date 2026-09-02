package com.skyblockextras;

import com.skyblockextras.config.SbeConfig;
import com.skyblockextras.pet.PetOverlay;
import com.skyblockextras.rng.RngTracker;
import com.skyblockextras.screen.SbeScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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

        System.out.println("[SBE] Registering client commands...");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            System.out.println("[SBE] Client command dispatcher initialized.");

            dispatcher.register(
                    ClientCommands.literal("sbe")
                            .executes(context -> openSettings(context.getSource().getClient()))
                            .then(ClientCommands.literal("gui")
                                    .executes(context -> openSettings(context.getSource().getClient())))
                            .then(ClientCommands.literal("reload")
                                    .executes(context -> {
                                        CONFIG = SbeConfig.load();
                                        RNG = new RngTracker(CONFIG);
                                        PET = new PetOverlay(CONFIG);
                                        context.getSource().sendFeedback(
                                                Component.literal("[SBE] Configuration reloaded."));
                                        return 1;
                                    }))
            );
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (message == null || overlay) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (RNG != null) RNG.handle(message);
        });

        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {
                    if (message == null) return;
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player == null) return;
                    if (RNG != null) RNG.handle(message);
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (PET != null) PET.tick(client);
        });

        System.out.println("[SBE] Skyblock Extras initialized.");
    }

    private static int openSettings(Minecraft minecraft) {
        if (minecraft == null) return 0;

        System.out.println("[SBE] /sbe executed - opening settings GUI.");
        minecraft.gui.setScreen(new SbeScreen(null));
        return 1;
    }
}
