package com.beatbox.player.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

@Entity(tableName = "play_history")
public class PlayHistoryEntity {
    @NonNull @PrimaryKey public String mediaId;
    public String uri;
    public String title;
    public String artist;
    public String album;
    public long durationMs;
    public String artworkUri;
    public long lastPlayedAt;
    public int playCount;

    public PlayHistoryEntity() { }
    @Ignore public PlayHistoryEntity(TrackRecord track, long playedAt, int playCount) {
        mediaId = track.mediaId; uri = track.uri; title = track.title;
        artist = track.artist; album = track.album; durationMs = track.durationMs; artworkUri=track.artworkUri;
        lastPlayedAt = playedAt; this.playCount = playCount;
    }
}
