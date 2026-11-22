package com.beatbox.player.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_tracks",
        foreignKeys = @ForeignKey(entity = PlaylistEntity.class, parentColumns = "id", childColumns = "playlistId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("playlistId"), @Index(value = {"playlistId", "position"})})
public class PlaylistTrackEntity {
    @PrimaryKey(autoGenerate = true) public long entryId;
    public long playlistId;
    public String mediaId;
    public String uri;
    public String title;
    public String artist;
    public String album;
    public long durationMs;
    public String artworkUri;
    public int position;

    public PlaylistTrackEntity() { }
    @Ignore public PlaylistTrackEntity(long playlistId, TrackRecord track, int position) {
        this.playlistId = playlistId; this.mediaId = track.mediaId; this.uri = track.uri;
        this.title = track.title; this.artist = track.artist; this.album = track.album;
        this.durationMs = track.durationMs; this.artworkUri=track.artworkUri; this.position = position;
    }
}
