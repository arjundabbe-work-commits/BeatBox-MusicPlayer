package com.beatbox.player.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class RecommendationAlgorithmsTest {
    private static final class Track implements TrackMetadata {
        final long id; final String title, artist, album;
        Track(long id, String title, String artist, String album) { this.id=id; this.title=title; this.artist=artist; this.album=album; }
        public long stableId(){return id;} public String title(){return title;} public String artist(){return artist;}
        public String album(){return album;} public long durationMs(){return 180_000;}
    }
    private final List<Track> library = Arrays.asList(new Track(1,"A","Jazz Group","One"), new Track(2,"B","Jazz Group","Two"), new Track(3,"C","Pop Artist","Three"));

    @Test public void newUserGetsStableLibraryFallbackAndNoHistoryIsSafe() {
        List<Track> first = RecommendationAlgorithms.recommend(library, Collections.emptyMap(), Collections.emptySet(), -1, 1_000_000, 10);
        List<Track> second = RecommendationAlgorithms.recommend(library, null, null, -1, 1_000_000, 10);
        assertEquals(Arrays.asList(1L,2L,3L), ids(first));
        assertEquals(ids(first), ids(second));
    }
    @Test public void favoritesAndRecentListeningShapeResultsAndPlayingTrackIsExcluded() {
        Map<Long,RecommendationAlgorithms.ListeningSignal> history = new HashMap<>();
        history.put(1L, new RecommendationAlgorithms.ListeningSignal(8, 990_000, 0));
        List<Track> picks=RecommendationAlgorithms.recommend(library, history, new HashSet<>(Collections.singletonList(3L)), 1, 1_000_000, 10);
        assertEquals(2, picks.size());
        assertEquals(3L, picks.get(0).stableId());
        assertTrue(picks.stream().anyMatch(track -> track.stableId() == 2L));
    }
    @Test public void skipsReduceTrackAffinityAndLimitIsRespected() {
        Map<Long,RecommendationAlgorithms.ListeningSignal> history = new HashMap<>();
        history.put(1L, new RecommendationAlgorithms.ListeningSignal(10, 999_000, 6));
        assertEquals(1, RecommendationAlgorithms.recommend(library, history, Collections.emptySet(), -1, 1_000_000, 1).size());
    }
    private static List<Long> ids(List<Track> tracks) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        for (Track track : tracks) ids.add(track.stableId());
        return ids;
    }
}
