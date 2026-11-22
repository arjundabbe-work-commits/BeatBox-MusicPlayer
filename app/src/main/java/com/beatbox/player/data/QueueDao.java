package com.beatbox.player.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class QueueDao {
    @Query("DELETE FROM queue_tracks") public abstract void clear();
    @Insert protected abstract void insertAll(List<QueueTrackEntity> tracks);
    @Query("SELECT * FROM queue_tracks ORDER BY position") public abstract List<QueueTrackEntity> getAll();

    @Transaction public void replace(List<TrackRecord> tracks) {
        clear();
        List<QueueTrackEntity> rows = new ArrayList<>();
        for (int i = 0; i < tracks.size(); i++) rows.add(new QueueTrackEntity(tracks.get(i), i));
        if (!rows.isEmpty()) insertAll(rows);
    }
}
