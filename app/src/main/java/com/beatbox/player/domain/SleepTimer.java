package com.beatbox.player.domain;

public final class SleepTimer {
    private SleepTimer() { }

    public static long deadlineAfter(long nowMs, long durationMs) {
        if (durationMs <= 0) throw new IllegalArgumentException("Timer duration must be positive");
        if (Long.MAX_VALUE - nowMs < durationMs) return Long.MAX_VALUE;
        return nowMs + durationMs;
    }

    public static long remaining(long deadlineMs, long nowMs) {
        if (deadlineMs <= nowMs) return 0;
        return deadlineMs - nowMs;
    }
}
