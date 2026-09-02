package com.skyblockextras.rng;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import com.skyblockextras.config.SbeConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RngTracker {

    private final SbeConfig config;

    /*
     * Item name -> last drop timestamp.
     *
     * The actual timestamps are stored in SbeConfig so they
     * survive Minecraft restarts.
     */
    private final Map<String, Long> lastDrops = new LinkedHashMap<>();

    /*
     * Every item that SBE should track.
     *
     * These are loaded from:
     *
     * src/main/resources/rng_drops.json
     */
    private final Map<String, String> exactDrops = new LinkedHashMap<>();

    private static final Gson GSON = new Gson();


    public RngTracker(SbeConfig config) {

        this.config = config;

        /*
         * Load timestamps saved from previous sessions.
         */
        if (config.lastDrops != null) {
            lastDrops.putAll(config.lastDrops);
        }

        /*
         * Load the RNG database.
         */
        loadDrops();
    }


    // ============================================================
    // LOAD RNG DATABASE
    // ============================================================

    private void loadDrops() {

        exactDrops.clear();

        try (InputStream stream =
                     RngTracker.class
                             .getClassLoader()
                             .getResourceAsStream("rng_drops.json")) {

            if (stream == null) {

                System.err.println(
                        "[SBE RNG] Could not find rng_drops.json"
                );

                return;
            }

            JsonObject root =
                    GSON.fromJson(
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            ),
                            JsonObject.class
                    );

            if (root == null) {
                return;
            }

            /*
             * Farming
             */
            JsonObject farming =
                    getObject(root, "farming");

            if (farming != null) {

                /*
                 * Harvest Feast
                 */
                if (config.harvestFeastEnabled) {

                    addArray(
                            farming,
                            "harvestFeast"
                    );
                }

                /*
                 * Farming dyes
                 */
                if (config.dyesEnabled) {

                    addArray(
                            farming,
                            "farmingDyes"
                    );
                }

                /*
                 * Slugs
                 */
                if (config.slugEnabled) {

                    addArray(
                            farming,
                            "slugs"
                    );
                }
            }


            /*
             * Mining
             */
            JsonObject mining =
                    getObject(root, "mining");

            if (mining != null) {

                addArray(
                        mining,
                        "rareDrops"
                );

                addArray(
                        mining,
                        "dyes"
                );
            }


            /*
             * Fishing
             */
            JsonObject fishing =
                    getObject(root, "fishing");

            if (fishing != null) {

                addArray(
                        fishing,
                        "rareDrops"
                );

                addArray(
                        fishing,
                        "dyes"
                );
            }


            /*
             * Combat
             */
            JsonObject combat =
                    getObject(root, "combat");

            if (combat != null) {

                addArray(
                        combat,
                        "rareDrops"
                );

                addArray(
                        combat,
                        "dyes"
                );
            }


            /*
             * Other
             */
            JsonObject other =
                    getObject(root, "other");

            if (other != null) {

                addArray(
                        other,
                        "rareDrops"
                );

                addArray(
                        other,
                        "dyes"
                );
            }


            System.out.println(
                    "[SBE RNG] Loaded "
                            + exactDrops.size()
                            + " trackable RNG drops."
            );

        } catch (Exception e) {

            System.err.println(
                    "[SBE RNG] Failed to load rng_drops.json:"
            );

            e.printStackTrace();
        }
    }


    // ============================================================
    // JSON HELPERS
    // ============================================================

    private JsonObject getObject(
            JsonObject parent,
            String name
    ) {

        if (!parent.has(name)) {
            return null;
        }

        JsonElement element =
                parent.get(name);

        if (element == null ||
                !element.isJsonObject()) {

            return null;
        }

        return element.getAsJsonObject();
    }


    private void addArray(
            JsonObject parent,
            String name
    ) {

        if (!parent.has(name)) {
            return;
        }

        JsonElement element =
                parent.get(name);

        if (element == null ||
                !element.isJsonArray()) {

            return;
        }

        JsonArray array =
                element.getAsJsonArray();

        for (JsonElement entry : array) {

            if (entry == null ||
                    !entry.isJsonPrimitive()) {

                continue;
            }

            String item =
                    entry.getAsString();

            if (item == null ||
                    item.isBlank()) {

                continue;
            }

            item = item.trim();

            exactDrops.put(
                    item,
                    item
            );
        }
    }


    // ============================================================
    // HANDLE CHAT MESSAGE
    // ============================================================

    public void handle(Component message) {

        if (message == null) {
            return;
        }

        /*
         * Master Farming RNG switch.
         */
        if (!config.farmingRngEnabled) {
            return;
        }

        String raw =
                message.getString();

        if (raw == null ||
                raw.isBlank()) {

            return;
        }

        String normalized =
                stripMinecraftFormatting(raw).trim();

        /*
         * We only care about messages that look like
         * an actual RNG/drop announcement.
         */
        boolean dropMessage =
                Pattern.compile(
                        "(?i)(RARE DROP|PRAY RNGESUS|CRAZY RARE|PET DROP|DROP)"
                )
                .matcher(normalized)
                .find();

        if (!dropMessage) {
            return;
        }


        /*
         * Check every item in rng_drops.json.
         *
         * No item outside the JSON can trigger the tracker.
         */
        for (String item : exactDrops.keySet()) {

            if (!containsWholePhrase(
                    normalized,
                    item
            )) {

                continue;
            }

            recordDrop(item);

            /*
             * One chat message should only produce
             * one RNG result.
             */
            break;
        }
    }


    // ============================================================
    // RECORD DROP
    // ============================================================

    private void recordDrop(String item) {

        long now =
                System.currentTimeMillis();

        Long previous =
                lastDrops.put(
                        item,
                        now
                );


        /*
         * Persist timestamp.
         */
        config.setLastDrop(
                item,
                now
        );

        config.save();


        String elapsed;

        if (previous == null) {

            elapsed =
                    "first tracked drop";

        } else {

            elapsed =
                    format(
                            now - previous
                    );
        }


        Minecraft client =
                Minecraft.getInstance();

        if (client.player != null) {

            String message =
                    "[SBE] "
                            + item
                            + " — "
                            + elapsed
                            + " since last "
                            + item;

            client.player.sendSystemMessage(
                    Component.literal(message)
            );
        }


        System.out.println(
                "[SBE RNG] "
                        + item
                        + " detected."
        );
    }


    // ============================================================
    // WHOLE PHRASE MATCHING
    // ============================================================

    private boolean containsWholePhrase(
            String text,
            String phrase
    ) {

        if (text == null ||
                phrase == null ||
                phrase.isBlank()) {

            return false;
        }

        String regex =
                "(?i)(?<![A-Za-z0-9_])"
                        + Pattern.quote(phrase)
                        + "(?![A-Za-z0-9_])";

        return Pattern.compile(regex)
                .matcher(text)
                .find();
    }


    // ============================================================
    // FORMAT TIME
    // ============================================================

    private String format(
            long milliseconds
    ) {

        Duration duration =
                Duration.ofMillis(
                        milliseconds
                );

        long days =
                duration.toDays();

        long hours =
                duration.toHoursPart();

        long minutes =
                duration.toMinutesPart();

        long seconds =
                duration.toSecondsPart();


        if (days > 0) {

            return String.format(
                    "%dd %02dh %02dm %02ds",
                    days,
                    hours,
                    minutes,
                    seconds
            );
        }


        return String.format(
                "%02dh %02dm %02ds",
                hours,
                minutes,
                seconds
        );
    }


    // ============================================================
    // STRIP MINECRAFT FORMATTING
    // ============================================================

    private String stripMinecraftFormatting(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replaceAll("§.", "")
                .replaceAll("\\u00A7.", "")
                .trim();
    }


    // ============================================================
    // GET TIMER
    // ============================================================

    public Long getLastDrop(
            String item
    ) {

        Long timestamp =
                lastDrops.get(item);

        if (timestamp != null) {
            return timestamp;
        }

        return config.getLastDrop(item);
    }


    // ============================================================
    // CLEAR ONE TIMER
    // ============================================================

    public void clearTimer(
            String item
    ) {

        lastDrops.remove(item);

        config.removeLastDrop(item);

        config.save();
    }


    // ============================================================
    // CLEAR ALL TIMERS
    // ============================================================

    public void clearAllTimers() {

        lastDrops.clear();

        config.clearLastDrops();

        config.save();
    }
}
