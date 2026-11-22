package com.beatbox.player.domain;

import java.util.Arrays;

/** Five normalized controls are mapped onto the bands exposed by a device effect. */
public final class EqualizerSettings {
    public static final int BAND_COUNT = 5;
    public static final int GAIN_MIN = -100;
    public static final int GAIN_MAX = 100;
    private EqualizerSettings() { }

    public static int clampGain(int value) { return Math.max(GAIN_MIN, Math.min(GAIN_MAX, value)); }

    public static int[] preset(String name) {
        if ("Bass".equals(name)) return new int[]{75, 55, 15, 0, 0};
        if ("Vocal".equals(name)) return new int[]{-20, 5, 50, 35, 10};
        if ("Rock".equals(name)) return new int[]{55, 30, -10, 35, 55};
        if ("Classical".equals(name)) return new int[]{35, 15, 0, 20, 40};
        return new int[BAND_COUNT];
    }

    public static int gainForDeviceBand(int[] normalizedGains, int band, int deviceBandCount) {
        if (normalizedGains == null || normalizedGains.length == 0 || band < 0 || deviceBandCount <= 0 || band >= deviceBandCount) return 0;
        int index = Math.min(normalizedGains.length - 1, (int) (((long) band * normalizedGains.length) / deviceBandCount));
        return clampGain(normalizedGains[index]);
    }

    public static short toMillibels(int normalizedGain, short[] range) {
        if (range == null || range.length < 2) return 0;
        int gain = clampGain(normalizedGain);
        int minimum = range[0], maximum = range[1];
        if (gain < 0) return (short) (minimum * (-(long) gain) / 100L);
        return (short) (maximum * (long) gain / 100L);
    }

    public static int[] copyPreset(String name) { return Arrays.copyOf(preset(name), BAND_COUNT); }
}
