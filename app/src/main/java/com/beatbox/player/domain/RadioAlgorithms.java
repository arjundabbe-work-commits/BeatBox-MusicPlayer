package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.Collections;
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
        Collections.sort(remaining, (left, right) -> {
            int comparison = Boolean.compare(!same(left.artist(), seed.artist()), !same(right.artist(), seed.artist()));
            if (comparison == 0) comparison = Boolean.compare(!same(left.album(), seed.album()), !same(right.album(), seed.album()));
            if (comparison == 0) comparison = safe(left.artist()).compareTo(safe(right.artist()));
            if (comparison == 0) comparison = safe(left.title()).compareTo(safe(right.title()));
            if (comparison == 0) comparison = Long.compare(left.stableId(), right.stableId());
            return comparison;
        });
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
