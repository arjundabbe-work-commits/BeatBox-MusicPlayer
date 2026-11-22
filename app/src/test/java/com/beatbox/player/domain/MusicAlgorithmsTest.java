package com.beatbox.player.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MusicAlgorithmsTest {
    private static final class TestTrack implements TrackMetadata {
        final long id; final String title, artist, album; final long duration;
        TestTrack(long id, String title, String artist, String album, long duration) { this.id=id; this.title=title; this.artist=artist; this.album=album; this.duration=duration; }
        public long stableId(){return id;} public String title(){return title;} public String artist(){return artist;} public String album(){return album;} public long durationMs(){return duration;}
    }
    private TestTrack song(long id, String title, String artist, String album, long duration) {
        return new TestTrack(id, title, artist, album, duration);
    }

    @Test public void searchMatchesTitleArtistAndAlbumCaseInsensitively() {
        List<TestTrack> songs = Arrays.asList(song(1,"Blue Sky","Mira","Morning",90000), song(2,"Night","Blue Trio","Live",120000), song(3,"Dawn","Ravi","Blue Notes",80000));
        assertEquals(3, MusicAlgorithms.search(songs," blue ").size());
        assertTrue(MusicAlgorithms.search(songs,"missing").isEmpty());
        assertTrue(MusicAlgorithms.search(songs," ").isEmpty());
        assertTrue(MusicAlgorithms.search(songs,null).isEmpty());
    }

    @Test public void sortsByRequestedMetadataWithoutMutatingInput() {
        List<TestTrack> songs = Arrays.asList(song(1,"Zebra","Mira","B",90000), song(2,"Alpha","Asha","C",120000), song(3,"Beta","Mira","A",80000));
        assertEquals("Alpha", MusicAlgorithms.sort(songs,MusicAlgorithms.Sort.TITLE).get(0).title);
        assertEquals("Asha", MusicAlgorithms.sort(songs,MusicAlgorithms.Sort.ARTIST).get(0).artist);
        assertEquals("A", MusicAlgorithms.sort(songs,MusicAlgorithms.Sort.ALBUM).get(0).album);
        assertEquals(80000, MusicAlgorithms.sort(songs,MusicAlgorithms.Sort.DURATION).get(0).duration);
        assertEquals("Zebra", songs.get(0).title);
    }

    @Test public void formatsDurationAndClampsNegativeValues() {
        assertEquals("0:00", MusicAlgorithms.formatTime(-1));
        assertEquals("1:05", MusicAlgorithms.formatTime(65000));
        assertEquals("61:01", MusicAlgorithms.formatTime(3661000));
    }
}
