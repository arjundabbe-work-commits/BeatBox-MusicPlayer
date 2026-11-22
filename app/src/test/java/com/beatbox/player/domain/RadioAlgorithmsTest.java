package com.beatbox.player.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class RadioAlgorithmsTest {
    private static final class Track implements TrackMetadata {
        final long id; final String title, artist, album;
        Track(long id, String title, String artist, String album) { this.id=id; this.title=title; this.artist=artist; this.album=album; }
        public long stableId(){return id;} public String title(){return title;} public String artist(){return artist;} public String album(){return album;} public long durationMs(){return 1000;}
    }

    @Test public void putsSeedFirstThenPrioritizesItsArtistAndAlbum() {
        Track seed = new Track(1,"One","Band","Record A");
        Track album = new Track(2,"Two","Band","Record A");
        Track artist = new Track(3,"Three","Band","Record B");
        Track other = new Track(4,"Four","Elsewhere","Elsewhere");
        List<Track> station = RadioAlgorithms.fromSeed(Arrays.asList(other, artist, album, seed), seed, 4);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), Arrays.asList(station.get(0).id, station.get(1).id, station.get(2).id, station.get(3).id));
    }

    @Test public void capsLengthAndHandlesEmptyLibrary() {
        Track seed = new Track(1,"One","Band","Record A");
        assertEquals(1, RadioAlgorithms.fromSeed(Arrays.asList(seed), seed, 20).size());
        assertTrue(RadioAlgorithms.fromSeed(Arrays.asList(seed), seed, 0).isEmpty());
    }

    @Test public void artistAndAlbumStationsPrioritizeMatchingLocalTracksAndDeduplicate() {
        Track seed = new Track(1,"One","Band","Record A");
        Track sameArtist = new Track(2,"Two","Band","Record B");
        Track sameAlbum = new Track(3,"Three","Elsewhere","Record A");
        Track other = new Track(4,"Four","Elsewhere","Record C");
        List<Track> library = Arrays.asList(other, sameArtist, seed, sameArtist, sameAlbum);
        assertEquals(Arrays.asList(1L,2L,4L,3L), ids(RadioAlgorithms.fromArtist(library," band ",8)));
        assertEquals(Arrays.asList(1L,3L,2L,4L), ids(RadioAlgorithms.fromAlbum(library,"RECORD A",8)));
    }

    @Test public void genreStationUsesCallerMembershipAndHandlesMissingTags() {
        Track one = new Track(1,"One","Band","A");
        Track two = new Track(2,"Two","Another","B");
        Track three = new Track(3,"Three","Zed","C");
        java.util.Map<Long,String> genres = new java.util.HashMap<>();
        genres.put(1L,"Rock"); genres.put(3L,"Rock");
        List<Track> station = RadioAlgorithms.fromGenre(Arrays.asList(two,three,one),"rock",track -> genres.get(track.id),2);
        assertEquals(Arrays.asList(1L,3L),ids(station));
        assertEquals(0,RadioAlgorithms.fromGenre(Arrays.asList(one),"",track -> "Rock",10).size());
    }

    private static List<Long> ids(List<Track> tracks) {
        List<Long> ids = new ArrayList<>();
        for (Track track : tracks) ids.add(track.id);
        return ids;
    }
}
