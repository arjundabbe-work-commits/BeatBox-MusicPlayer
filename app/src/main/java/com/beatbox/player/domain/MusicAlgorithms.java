package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MusicAlgorithms {
    public enum Sort { TITLE, ARTIST, ALBUM, DURATION }
    private MusicAlgorithms() { }

    public static <T extends TrackMetadata> List<T> search(List<T> songs, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        List<T> result = new ArrayList<>();
        if (query.isEmpty()) return result;
        for (T song : songs) {
            if ((song.title() + " " + song.artist() + " " + song.album()).toLowerCase(Locale.ROOT).contains(query)) result.add(song);
        }
        return result;
    }

    public static <T extends TrackMetadata> List<T> sort(List<T> songs, Sort sort) {
        List<T> result = new ArrayList<>(songs);
        Comparator<T> comparator;
        switch (sort) {
            case ARTIST: comparator = Comparator.comparing(s -> s.artist().toLowerCase(Locale.ROOT)); break;
            case ALBUM: comparator = Comparator.comparing(s -> s.album().toLowerCase(Locale.ROOT)); break;
            case DURATION: comparator = Comparator.comparingLong(TrackMetadata::durationMs); break;
            default: comparator = Comparator.comparing(s -> s.title().toLowerCase(Locale.ROOT));
        }
        result.sort(comparator.thenComparingLong(TrackMetadata::stableId));
        return result;
    }

    public static String formatTime(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
