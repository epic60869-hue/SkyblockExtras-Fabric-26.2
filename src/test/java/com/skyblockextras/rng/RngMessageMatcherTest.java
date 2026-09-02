package com.skyblockextras.rng;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RngMessageMatcherTest {

    private static final List<String> FEAST_DROPS = List.of(
            "Aggourdian",
            "Botroot",
            "Cactus Flower",
            "Cane Knot",
            "Carrot Zest",
            "Cornucopia",
            "Cropie",
            "Crystalized Moonlight",
            "Deepfries",
            "Designer Coffee Beans",
            "Feastfungus",
            "Fermento",
            "Floral Gelatin",
            "Helianthus",
            "Melon Juice",
            "Salted Sunflower Seeds",
            "Squash"
    );

    @Test
    void detectsExactHarvestFeastDrop() {
        assertEquals(
                "Squash",
                RngMessageMatcher.findDrop(
                        "RARE DROP! You found a Squash!",
                        FEAST_DROPS
                )
        );
    }

    @Test
    void detectsMultiWordDrop() {
        assertEquals(
                "Designer Coffee Beans",
                RngMessageMatcher.findDrop(
                        "§6PRAY RNGESUS! §fDesigner Coffee Beans",
                        FEAST_DROPS
                )
        );
    }

    @Test
    void ignoresNormalChat() {
        assertNull(
                RngMessageMatcher.findDrop(
                        "I got Squash from farming",
                        FEAST_DROPS
                )
        );
    }

    @Test
    void doesNotMatchPartialWords() {
        assertTrue(!RngMessageMatcher.containsWholePhrase("RARE DROP! NotCropie", "Cropie"));
    }

    @Test
    void stripsMinecraftFormatting() {
        assertEquals(
                "RARE DROP! Squash",
                RngMessageMatcher.stripMinecraftFormatting("§6RARE DROP! §aSquash")
        );
    }
}
