package com.beatbox.player.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public abstract class FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) public abstract void save(FavoriteEntity favorite);
    @Query("DELETE FROM favorites WHERE mediaId = :mediaId") public abstract int remove(String mediaId);
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)") public abstract boolean contains(String mediaId);
    @Query("SELECT * FROM favorites ORDER BY savedAt DESC") public abstract List<FavoriteEntity> getAll();
    @Query("DELETE FROM favorites") public abstract void clear();
    @androidx.room.Transaction public boolean toggle(FavoriteEntity favorite) {
        if (contains(favorite.mediaId)) { remove(favorite.mediaId); return false; }
        save(favorite); return true;
    }
}
