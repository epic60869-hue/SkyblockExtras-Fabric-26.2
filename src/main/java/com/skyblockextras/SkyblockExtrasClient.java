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

        // Load configuration
        CONFIG = SbeConfig.load();

        // Initialise systems
        RNG = new RngTracker(CONFIG);
        PET = new PetOverlay(CONFIG);

        /*
         * /sbe
         *
         * Opens the Skyblock Extras settings menu.
         */
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {

            dispatcher.register(
                    ClientCommands.literal("sbe")

                            // /sbe
                            .executes(context -> {
                                Minecraft.getInstance().setScreen(
                                        new SbeScreen(null)
                                );
                                return 1;
                            })

                            // /sbe gui
                            .then(
                                    ClientCommands.literal("gui")
                                            .executes(context -> {
                                                Minecraft.getInstance().setScreen(
                                                        new SbeScreen(null)
                                                );
                                                return 1;
                                            })
                            )

                            // /sbe reload
                            .then(
                                    ClientCommands.literal("reload")
                                            .executes(context -> {

                                                CONFIG = SbeConfig.load();
                                                RNG = new RngTracker(CONFIG);
                                                PET = new PetOverlay(CONFIG);

                                                if (Minecraft.getInstance().player != null) {
                                                    Minecraft.getInstance().player.sendSystemMessage(
                                                            Component.literal(
                                                                    "[SBE] Configuration reloaded."
                                                            )
                                                    );
                                                }

                                                return 1;
                                            })
                            )
            );
        });

        /*
         * Receive normal game/system messages.
         */
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {

            if (!overlay && Minecraft.getInstance().player != null) {
                RNG.handle(message);
            }
        });

        /*
         * Receive chat messages.
         */
        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {

                    if (Minecraft.getInstance().player != null) {
                        RNG.handle(message);
                    }
                }
        );

        /*
         * Client tick.
         */
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (PET != null) {
                PET.tick(client);
            }
        });
    }
}
