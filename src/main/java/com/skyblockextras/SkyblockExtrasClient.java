package com.skyblockextras;

import com.mojang.authlib.GameProfile;
import com.skyblockextras.config.SbeConfig;
import com.skyblockextras.pet.PetOverlay;
import com.skyblockextras.rng.RngDropOverlay;
import com.skyblockextras.rng.RngTracker;
import com.skyblockextras.screen.SbeSkyHanniScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SkyblockExtrasClient implements ClientModInitializer {
    public static final String MOD_ID = "skyblockextras";
    public static SbeConfig CONFIG;
    public static RngTracker RNG;
    public static PetOverlay PET;
    public static RngDropOverlay RNG_DROP_OVERLAY;

    @Override
    public void onInitializeClient() {
        CONFIG = SbeConfig.load();
        RNG = new RngTracker(CONFIG);
        PET = new PetOverlay(CONFIG);
        RNG_DROP_OVERLAY = new RngDropOverlay(CONFIG);

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
                                        RNG_DROP_OVERLAY = new RngDropOverlay(CONFIG);
                                        context.getSource().sendFeedback(Component.literal("[SBE] Configuration reloaded."));
                                        return 1;
                                    }))
            );
        });

        // Hypixel RNG announcements are normally delivered as game/system messages.
        // Keep GAME handling as the authoritative drop source.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (message == null || overlay) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (RNG != null) RNG.handle(message);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (message == null) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            // A copied RNG announcement pasted back into chat is now a normal
            // player chat message. Never count the player's own pasted message
            // as a new drop, or the per-item RNG timer would reset.
            if (isOwnChatMessage(sender, minecraft)) return;

            if (RNG != null) RNG.handle(message);
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "pet_overlay"),
                (graphics, deltaTracker) -> {
                    if (PET != null) PET.render(graphics, deltaTracker);
                }
        );

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "rng_drop_overlay"),
                (graphics, deltaTracker) -> {
                    if (RNG_DROP_OVERLAY != null) RNG_DROP_OVERLAY.render(graphics, deltaTracker);
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (PET != null) PET.tick(client);
        });

        System.out.println("[SBE] Skyblock Extras initialized.");
    }

    private static boolean isOwnChatMessage(GameProfile sender, Minecraft minecraft) {
        if (sender == null || minecraft.player == null) return false;
        try {
            return sender.getId() != null && sender.getId().equals(minecraft.player.getUUID());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int openSettings(Minecraft minecraft) {
        if (minecraft == null) return 0;
        minecraft.execute(() -> minecraft.gui.setScreen(new SbeSkyHanniScreen(null)));
        return 1;
    }
}
