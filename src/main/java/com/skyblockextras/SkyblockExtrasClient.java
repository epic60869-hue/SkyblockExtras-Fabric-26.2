package com.skyblockextras;

import com.skyblockextras.config.SbeConfig;
import com.skyblockextras.pet.PetOverlay;
import com.skyblockextras.rng.DiscordWebhook;
import com.skyblockextras.rng.RngDropOverlay;
import com.skyblockextras.rng.RngTracker;
import com.skyblockextras.screen.SbeSkyHanniScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class SkyblockExtrasClient implements ClientModInitializer {
    public static final String MOD_ID = "skyblockextras";
    public static SbeConfig CONFIG;
    public static RngTracker RNG;
    public static PetOverlay PET;
    public static RngDropOverlay RNG_DROP_OVERLAY;
    public static DiscordWebhook DISCORD_WEBHOOK;

    // Created exactly once per Minecraft process, so /sbe reload does not reset the session timer.
    public static final long SESSION_START = System.currentTimeMillis();

    @Override
    public void onInitializeClient() {
        CONFIG = SbeConfig.load();
        RNG = new RngTracker(CONFIG);
        PET = new PetOverlay(CONFIG);
        RNG_DROP_OVERLAY = new RngDropOverlay(CONFIG);
        DISCORD_WEBHOOK = new DiscordWebhook(CONFIG, SESSION_START);

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
                                        DISCORD_WEBHOOK = new DiscordWebhook(CONFIG, SESSION_START);
                                        context.getSource().sendFeedback(Component.literal("[SBE] Configuration reloaded."));
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

    private static int openSettings(Minecraft minecraft) {
        if (minecraft == null) return 0;
        minecraft.execute(() -> minecraft.gui.setScreen(new SbeSkyHanniScreen(null)));
        return 1;
    }
}