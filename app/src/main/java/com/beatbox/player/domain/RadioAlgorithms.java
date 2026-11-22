package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

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

    public static <T extends TrackMetadata> List<T> fromArtist(List<T> library, String artist, int limit) {
        return fromCategory(library, artist, TrackMetadata::artist, limit);
    }

    public static <T extends TrackMetadata> List<T> fromAlbum(List<T> library, String album, int limit) {
        return fromCategory(library, album, TrackMetadata::album, limit);
    }

    /** Genres come from MediaStore membership and are supplied without changing the track model. */
    public static <T extends TrackMetadata> List<T> fromGenre(List<T> library, String genre,
            Function<? super T, String> genreForTrack, int limit) {
        return fromCategory(library, genre, genreForTrack, limit);
    }

    private static <T extends TrackMetadata> List<T> fromCategory(List<T> library, String label,
            Function<? super T, String> categoryForTrack, int limit) {
        List<T> station = new ArrayList<>();
        if (library == null || label == null || label.trim().isEmpty() || categoryForTrack == null || limit <= 0) return station;
        List<T> candidates = new ArrayList<>();
        for (T track : library) if (track != null && !containsId(candidates, track.stableId())) candidates.add(track);
        final String key = safe(label);
        Collections.sort(candidates, (left, right) -> {
            int byMatch = Boolean.compare(!same(categoryForTrack.apply(left), key), !same(categoryForTrack.apply(right), key));
            if (byMatch != 0) return byMatch;
            int byArtist = safe(left.artist()).compareTo(safe(right.artist()));
            if (byArtist != 0) return byArtist;
            int byTitle = safe(left.title()).compareTo(safe(right.title()));
            return byTitle != 0 ? byTitle : Long.compare(left.stableId(), right.stableId());
        });
        for (T track : candidates) {
            if (station.size() == limit) break;
            station.add(track);
        }
        return station;
    }

    private static <T extends TrackMetadata> boolean containsId(List<T> tracks, long id) {
        for (T track : tracks) if (track.stableId() == id) return true;
        return false;
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
