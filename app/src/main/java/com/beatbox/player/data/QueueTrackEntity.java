package com.beatbox.player.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "queue_tracks")
public class QueueTrackEntity {
    @PrimaryKey(autoGenerate = true) public long entryId;
    public String mediaId;
    public String uri;
    public String title;
    public String artist;
    public String album;
    public long durationMs;
    public String artworkUri;
    public int position;

    public QueueTrackEntity() { }
    @Ignore public QueueTrackEntity(TrackRecord track, int position) {
        mediaId = track.mediaId; uri = track.uri; title = track.title;
        artist = track.artist; album = track.album; durationMs = track.durationMs; artworkUri=track.artworkUri; this.position = position;
    }
}
