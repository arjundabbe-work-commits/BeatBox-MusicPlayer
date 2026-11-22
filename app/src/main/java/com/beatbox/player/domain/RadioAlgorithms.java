package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RadioAlgorithms {
    private RadioAlgorithms() { }

    public static <T extends TrackMetadata> List<T> fromSeed(List<T> library, T seed, int limit) {
        List<T> station = new ArrayList<>();
        if (seed == null || limit <= 0) return station;
        station.add(seed);
        List<T> remaining = new ArrayList<>();
        for (T candidate : library) if (candidate.stableId() != seed.stableId()) remaining.add(candidate);
        remaining.sort(Comparator.comparing((T track) -> !same(track.artist(), seed.artist()))
                .thenComparing(track -> !same(track.album(), seed.album()))
                .thenComparing(track -> safe(track.artist()))
                .thenComparing(track -> safe(track.title()))
                .thenComparingLong(TrackMetadata::stableId));
        for (T candidate : remaining) {
            if (station.size() >= limit) break;
            station.add(candidate);
        }
        return station;
    }

    private static boolean same(String left, String right) {
        String a = safe(left), b = safe(right);
        return !a.isEmpty() && !a.equals("unknown") && !a.equals("unknown artist")
                && !a.equals("unknown album") && a.equals(b);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
