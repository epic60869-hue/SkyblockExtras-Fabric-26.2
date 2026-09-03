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
                        "RARE CROP! Squash",
                        FEAST_DROPS
                )
        );
    }

    @Test
    void detectsMultiWordDrop() {
        assertEquals(
                "Designer Coffee Beans",
                RngMessageMatcher.findDrop(
                        "§6RARE CROP! §aDesigner Coffee Beans",
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
    void ignoresOldRareDropFormat() {
        assertNull(
                RngMessageMatcher.findDrop(
                        "RARE DROP! Squash",
                        FEAST_DROPS
                )
        );
    }

    @Test
    void doesNotMatchPartialWords() {
        assertTrue(!RngMessageMatcher.containsWholePhrase("RARE CROP! NotCropie", "Cropie"));
    }

    @Test
    void stripsMinecraftFormatting() {
        assertEquals(
                "RARE CROP! Squash",
                RngMessageMatcher.stripMinecraftFormatting("§6RARE CROP! §aSquash")
        );
    }
}
