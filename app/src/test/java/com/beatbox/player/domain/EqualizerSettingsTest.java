package com.beatbox.player.domain;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class EqualizerSettingsTest {
    @Test public void presetsAreFiveBandAndFlatIsZeroed() {
        assertArrayEquals(new int[]{0,0,0,0,0}, EqualizerSettings.preset("Flat"));
        assertEquals(5, EqualizerSettings.preset("Rock").length);
        assertEquals(75, EqualizerSettings.preset("Bass")[0]);
    }

    @Test public void gainsAreClampedAndMappedToDeviceRange() {
        assertEquals(100, EqualizerSettings.clampGain(800));
        assertEquals(-1500, EqualizerSettings.toMillibels(-100, new short[]{-1500,1500}));
        assertEquals(750, EqualizerSettings.toMillibels(50, new short[]{-1500,1500}));
        assertEquals(-20, EqualizerSettings.gainForDeviceBand(new int[]{-20,0,20}, 0, 3));
        assertEquals(20, EqualizerSettings.gainForDeviceBand(new int[]{-20,0,20}, 2, 3));
    }
}
