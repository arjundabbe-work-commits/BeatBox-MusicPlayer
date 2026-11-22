package com.beatbox.player.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class PlaylistDao {
    @Insert public abstract long create(PlaylistEntity playlist);
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC, name COLLATE NOCASE") public abstract List<PlaylistEntity> getAll();
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1") public abstract PlaylistEntity get(long id);
    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :id") public abstract int rename(long id, String name, long updatedAt);
    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :id") public abstract int touch(long id, long updatedAt);
    @Query("DELETE FROM playlists WHERE id = :id") public abstract int delete(long id);
    @Insert public abstract long insertEntry(PlaylistTrackEntity entry);
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position, entryId") public abstract List<PlaylistTrackEntity> getTracks(long playlistId);
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId") public abstract int nextPosition(long playlistId);
    @Query("DELETE FROM playlist_tracks WHERE entryId = :entryId AND playlistId = :playlistId") protected abstract int deleteEntry(long playlistId, long entryId);
    @Query("UPDATE playlist_tracks SET position = :position WHERE entryId = :entryId") protected abstract int setPosition(long entryId, int position);

    @Transaction public long append(long playlistId, TrackRecord track) {
        if (get(playlistId) == null) throw new IllegalArgumentException("Playlist does not exist");
        long entry=insertEntry(new PlaylistTrackEntity(playlistId, track, nextPosition(playlistId)));
        touch(playlistId,System.currentTimeMillis());return entry;
    }

    @Transaction public int removeEntry(long playlistId,long entryId){
        int removed=deleteEntry(playlistId,entryId);
        if(removed==0)return 0;
        List<PlaylistTrackEntity> rows=getTracks(playlistId);
        for(int i=0;i<rows.size();i++)setPosition(rows.get(i).entryId,rows.size()+1+i);
        for(int i=0;i<rows.size();i++)setPosition(rows.get(i).entryId,i);
        touch(playlistId,System.currentTimeMillis());return removed;
    }

    @Transaction public boolean reorder(long playlistId, long entryId, int newPosition) {
        List<PlaylistTrackEntity> current = new ArrayList<>(getTracks(playlistId));
        int oldPosition = -1;
        PlaylistTrackEntity moving = null;
        for (int i = 0; i < current.size(); i++) if (current.get(i).entryId == entryId) { oldPosition = i; moving = current.remove(i); break; }
        if (moving == null || newPosition < 0 || newPosition >= current.size() + 1) return false;
        current.add(newPosition, moving);
        int offset = current.size() + 1;
        for (int i = 0; i < current.size(); i++) setPosition(current.get(i).entryId, offset + i);
        for (int i = 0; i < current.size(); i++) setPosition(current.get(i).entryId, i);
        touch(playlistId,System.currentTimeMillis());
        return oldPosition != newPosition;
    }
}
