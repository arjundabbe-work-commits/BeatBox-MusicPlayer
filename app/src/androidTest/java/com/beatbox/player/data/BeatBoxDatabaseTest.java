package com.beatbox.player.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BeatBoxDatabaseTest {
    private BeatBoxDatabase database;
    private final TrackRecord trackA = new TrackRecord("a","content://music/a","A","Artist","Album",1000);
    private final TrackRecord trackB = new TrackRecord("b","content://music/b","B","Artist","Album",2000);

    @Before public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, BeatBoxDatabase.class).allowMainThreadQueries().build();
    }
    @After public void closeDatabase() { database.close(); }

    @Test public void playlistAllowsDuplicateTracksAndReordersEntries() {
        long playlistId = database.playlists().create(new PlaylistEntity("Road",1,1));
        long first = database.playlists().append(playlistId,trackA);
        database.playlists().append(playlistId,trackA);
        long third = database.playlists().append(playlistId,trackB);
        assertTrue(database.playlists().reorder(playlistId,third,0));
        List<PlaylistTrackEntity> tracks = database.playlists().getTracks(playlistId);
        assertEquals(3,tracks.size());
        assertEquals("b",tracks.get(0).mediaId);
        assertEquals(first,tracks.get(1).entryId);
        assertEquals("a",tracks.get(2).mediaId);
    }

    @Test public void playlistDeleteCascadesToTrackEntries() {
        long playlistId = database.playlists().create(new PlaylistEntity("Delete me",1,1));
        database.playlists().append(playlistId,trackA);
        assertEquals(1,database.playlists().delete(playlistId));
        assertTrue(database.playlists().getTracks(playlistId).isEmpty());
    }

    @Test public void favoritesCanBeAddedRemovedAndReadAcrossDaoCalls() {
        database.favorites().save(new FavoriteEntity(trackA,10));
        assertTrue(database.favorites().contains("a"));
        assertEquals("A",database.favorites().getAll().get(0).title);
        assertEquals(1,database.favorites().remove("a"));
        assertFalse(database.favorites().contains("a"));
    }

    @Test public void historyCountsPlaysAndQueueIsReplacedAtomically() {
        database.history().record(trackA,100);
        database.history().record(trackA,200);
        assertEquals(2,database.history().recent(10).get(0).playCount);
        database.queue().replace(Arrays.asList(trackA,trackB));
        assertEquals(2,database.queue().getAll().size());
        assertEquals("b",database.queue().getAll().get(1).mediaId);
        database.queue().replace(Arrays.asList(trackB));
        assertEquals(1,database.queue().getAll().size());
        assertEquals("b",database.queue().getAll().get(0).mediaId);
    }

    @Test public void removingEntryKeepsPlaylistPositionsContiguousAndSettingsPersist() {
        long id=database.playlists().create(new PlaylistEntity("List",1,1));
        long first=database.playlists().append(id,trackA);
        long second=database.playlists().append(id,trackB);
        assertEquals(1,database.playlists().removeEntry(id,first));
        List<PlaylistTrackEntity> remaining=database.playlists().getTracks(id);
        assertEquals(1,remaining.size());assertEquals(second,remaining.get(0).entryId);assertEquals(0,remaining.get(0).position);
        database.settings().set("sort","ARTIST");assertEquals("ARTIST",database.settings().get("sort"));
        database.settings().set("audio_effect_preset","Rock");
        database.settings().set("audio_effect_band_0","55");
        assertEquals("Rock",database.settings().get("audio_effect_preset"));
        assertEquals("55",database.settings().get("audio_effect_band_0"));
        database.history().clear();assertTrue(database.history().recent(10).isEmpty());
    }
}
