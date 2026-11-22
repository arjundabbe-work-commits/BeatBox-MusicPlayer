package com.beatbox.player.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract void save(PlayHistoryEntity row);
    @Query("SELECT * FROM play_history WHERE mediaId = :mediaId LIMIT 1") protected abstract PlayHistoryEntity find(String mediaId);
    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC LIMIT :limit") public abstract List<PlayHistoryEntity> recent(int limit);
    @Query("DELETE FROM play_history") public abstract void clear();
    @Query("DELETE FROM play_history WHERE mediaId NOT IN (SELECT mediaId FROM play_history ORDER BY lastPlayedAt DESC LIMIT 500)") protected abstract void prune();

    @Transaction public void record(TrackRecord track, long playedAt) {
        PlayHistoryEntity existing = find(track.mediaId);
        int count = existing == null ? 1 : existing.playCount + 1;
        save(new PlayHistoryEntity(track, playedAt, count));
        prune();
    }
}
