package com.beatbox.player.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class LibraryAlgorithmsTest {
    @Test public void groupsCaseInsensitivelyAndKeepsUnknownMetadata() {
        List<String> tracks = Arrays.asList("one", "two", "three", "four");
        Map<String, List<String>> grouped = LibraryAlgorithms.groupBy(tracks, track -> {
            if (track.equals("one") || track.equals("two")) return "Jazz";
            if (track.equals("three")) return "jAZZ";
            return " ";
        });
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList("one", "two", "three"), grouped.get("Jazz"));
        assertEquals(Arrays.asList("four"), grouped.get("Unknown"));
        assertEquals("Jazz", grouped.keySet().iterator().next());
    }

    @Test public void emptyInputProducesNoGroups() {
        assertTrue(LibraryAlgorithms.groupBy(Arrays.asList(), value -> "unused").isEmpty());
    }
}
