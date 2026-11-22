package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Deterministic, offline recommendations based only on the device library and play history. */
public final class RecommendationAlgorithms {
    private RecommendationAlgorithms() { }

    public static final class ListeningSignal {
        public final int plays;
        public final long lastPlayedAtMs;
        public final int skips;
        public ListeningSignal(int plays, long lastPlayedAtMs, int skips) {
            this.plays = Math.max(0, plays);
            this.lastPlayedAtMs = Math.max(0, lastPlayedAtMs);
            this.skips = Math.max(0, skips);
        }
    }

    public static <T extends TrackMetadata> List<T> recommend(
            List<T> library, Map<Long, ListeningSignal> history, Set<Long> favorites,
            long currentTrackId, long nowMs, int limit) {
        List<T> result = new ArrayList<>();
        if (library == null || limit <= 0) return result;
        Map<String, Integer> artistAffinity = new HashMap<>();
        Map<String, Integer> albumAffinity = new HashMap<>();
        if (history != null) for (T track : library) {
            if (track == null) continue;
            ListeningSignal signal = history.get(track.stableId());
            if (signal == null) continue;
            int weight = preferenceWeight(signal, nowMs);
            addAffinity(artistAffinity, track.artist(), weight);
            addAffinity(albumAffinity, track.album(), weight);
        }
        Set<Long> seen = new HashSet<>();
        for (T track : library) {
            if (track == null || track.stableId() == currentTrackId || !seen.add(track.stableId())) continue;
            result.add(track);
        }
        Collections.sort(result, (left, right) -> {
            int byScore = Integer.compare(score(right, history, favorites, artistAffinity, albumAffinity, nowMs),
                    score(left, history, favorites, artistAffinity, albumAffinity, nowMs));
            if (byScore != 0) return byScore;
            int byTitle = safe(left.title()).compareTo(safe(right.title()));
            return byTitle != 0 ? byTitle : Long.compare(left.stableId(), right.stableId());
        });
        if (result.size() > limit) return new ArrayList<>(result.subList(0, limit));
        return result;
    }

    private static <T extends TrackMetadata> int score(T track, Map<Long, ListeningSignal> history,
            Set<Long> favorites, Map<String, Integer> artists, Map<String, Integer> albums, long nowMs) {
        int value = 0;
        if (favorites != null && favorites.contains(track.stableId())) value += 100;
        ListeningSignal own = history == null ? null : history.get(track.stableId());
        if (own != null) value += preferenceWeight(own, nowMs) - Math.min(60, own.skips * 20);
        value += Math.min(80, artists.getOrDefault(safe(track.artist()), 0));
        value += Math.min(60, albums.getOrDefault(safe(track.album()), 0));
        return value;
    }

    private static int preferenceWeight(ListeningSignal signal, long nowMs) {
        long ageDays = signal.lastPlayedAtMs == 0 || nowMs <= signal.lastPlayedAtMs ? 365
                : (nowMs - signal.lastPlayedAtMs) / 86_400_000L;
        int recency = ageDays <= 1 ? 30 : ageDays <= 7 ? 20 : ageDays <= 30 ? 10 : ageDays <= 90 ? 4 : 0;
        return Math.min(50, signal.plays * 5) + recency - Math.min(30, signal.skips * 10);
    }

    private static void addAffinity(Map<String, Integer> scores, String raw, int weight) {
        String key = safe(raw);
        if (!key.isEmpty() && !key.startsWith("unknown")) scores.put(key, scores.getOrDefault(key, 0) + weight);
    }

    private static String safe(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
