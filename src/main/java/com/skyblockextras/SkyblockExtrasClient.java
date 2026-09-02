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

        // =========================================================
        // LOAD SBE CONFIGURATION
        // =========================================================

        CONFIG = SbeConfig.load();

        // =========================================================
        // INITIALISE SBE SYSTEMS
        // =========================================================

        RNG = new RngTracker(CONFIG);
        PET = new PetOverlay(CONFIG);

        // =========================================================
        // SBE CLIENT COMMAND
        //
        // /sbe
        // /sbe gui
        // /sbe reload
        // =========================================================

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, buildContext) -> {

                    dispatcher.register(
                            ClientCommands.literal("sbe")

                                    // -------------------------------------------------
                                    // /sbe
                                    // -------------------------------------------------

                                    .executes(context -> {

                                        Minecraft.getInstance().gui.setScreen(
                                                new SbeScreen(null)
                                        );

                                        return 1;
                                    })

                                    // -------------------------------------------------
                                    // /sbe gui
                                    // -------------------------------------------------

                                    .then(
                                            ClientCommands.literal("gui")
                                                    .executes(context -> {

                                                        Minecraft.getInstance().gui.setScreen(
                                                                new SbeScreen(null)
                                                        );

                                                        return 1;
                                                    })
                                    )

                                    // -------------------------------------------------
                                    // /sbe reload
                                    // -------------------------------------------------

                                    .then(
                                            ClientCommands.literal("reload")
                                                    .executes(context -> {

                                                        CONFIG = SbeConfig.load();

                                                        RNG = new RngTracker(CONFIG);

                                                        PET = new PetOverlay(CONFIG);

                                                        Minecraft minecraft =
                                                                Minecraft.getInstance();

                                                        if (minecraft.player != null) {

                                                            minecraft.player.sendSystemMessage(
                                                                    Component.literal(
                                                                            "[SBE] Configuration reloaded."
                                                                    )
                                                            );
                                                        }

                                                        return 1;
                                                    })
                                    )
                    );
                }
        );

        // =========================================================
        // GAME / SYSTEM MESSAGES
        // =========================================================

        ClientReceiveMessageEvents.GAME.register(
                (message, overlay) -> {

                    if (message == null) {
                        return;
                    }

                    Minecraft minecraft = Minecraft.getInstance();

                    if (minecraft.player == null) {
                        return;
                    }

                    // Don't process overlay messages.
                    if (overlay) {
                        return;
                    }

                    if (RNG != null) {
                        RNG.handle(message);
                    }
                }
        );

        // =========================================================
        // CHAT MESSAGES
        // =========================================================

        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {

                    if (message == null) {
                        return;
                    }

                    Minecraft minecraft = Minecraft.getInstance();

                    if (minecraft.player == null) {
                        return;
                    }

                    if (RNG != null) {
                        RNG.handle(message);
                    }
                }
        );

        // =========================================================
        // CLIENT TICK
        // =========================================================

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {

                    if (PET != null) {
                        PET.tick(client);
                    }
                }
        );

        // =========================================================
        // INITIALISATION COMPLETE
        // =========================================================

        System.out.println("[SBE] Skyblock Extras initialized.");
    }
}
