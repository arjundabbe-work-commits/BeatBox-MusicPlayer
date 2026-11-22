package com.beatbox.player.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PlaylistEntity.class, PlaylistTrackEntity.class, FavoriteEntity.class,
        PlayHistoryEntity.class, QueueTrackEntity.class, SettingEntity.class}, version = 1, exportSchema = true)
public abstract class BeatBoxDatabase extends RoomDatabase {
    public abstract PlaylistDao playlists();
    public abstract FavoriteDao favorites();
    public abstract HistoryDao history();
    public abstract QueueDao queue();
    public abstract SettingsDao settings();
}
