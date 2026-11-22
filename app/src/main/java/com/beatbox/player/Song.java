package com.beatbox.player;

import android.net.Uri;
import com.beatbox.player.domain.TrackMetadata;

public final class Song implements TrackMetadata {
    public final long id;
    public final String title;
    public final String artist;
    public final String album;
    public final long duration;
    public final Uri uri;
    public final Uri artworkUri;

    public Song(long id, String title, String artist, String album, long duration, Uri uri) {
        this(id,title,artist,album,duration,uri,null);
    }
    public Song(long id, String title, String artist, String album, long duration, Uri uri, Uri artworkUri) {
        this.id = id; this.title = title; this.artist = artist; this.album = album; this.duration = duration; this.uri = uri; this.artworkUri=artworkUri;
    }
    @Override public String title() { return title; }
    @Override public String artist() { return artist; }
    @Override public String album() { return album; }
    @Override public long durationMs() { return duration; }
    @Override public long stableId() { return id; }
    @Override public String toString() { return title + "\n" + artist + "  •  " + album; }
}
