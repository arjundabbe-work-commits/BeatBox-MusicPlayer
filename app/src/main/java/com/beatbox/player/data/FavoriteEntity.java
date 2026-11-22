package com.beatbox.player.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "favorites", indices = {@Index("savedAt")})
public class FavoriteEntity {
    @PrimaryKey public String mediaId;
    public String uri;
    public String title;
    public String artist;
    public String album;
    public long durationMs;
    public String artworkUri;
    public long savedAt;

    public FavoriteEntity() { }
    @Ignore public FavoriteEntity(TrackRecord track, long savedAt) {
        mediaId = track.mediaId; uri = track.uri; title = track.title;
        artist = track.artist; album = track.album; durationMs = track.durationMs; artworkUri=track.artworkUri; this.savedAt = savedAt;
    }
}
