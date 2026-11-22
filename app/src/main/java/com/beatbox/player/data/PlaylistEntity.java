package com.beatbox.player.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public long createdAt;
    public long updatedAt;

    public PlaylistEntity() { }
    @Ignore public PlaylistEntity(String name, long createdAt, long updatedAt) {
        this.name = name; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
}
