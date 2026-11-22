package com.beatbox.player.data;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.beatbox.player.Song;

public final class TrackRecord {
    @NonNull public final String mediaId;
    @NonNull public final String uri;
    @NonNull public final String title;
    @NonNull public final String artist;
    @NonNull public final String album;
    public final long durationMs;
    public final String artworkUri;

    public TrackRecord(@NonNull String mediaId, @NonNull String uri, @NonNull String title,
                       @NonNull String artist, @NonNull String album, long durationMs) {
        this(mediaId,uri,title,artist,album,durationMs,null);
    }
    public TrackRecord(@NonNull String mediaId, @NonNull String uri, @NonNull String title,
                       @NonNull String artist, @NonNull String album, long durationMs, String artworkUri) {
        this.mediaId = mediaId; this.uri = uri; this.title = title;
        this.artist = artist; this.album = album; this.durationMs = durationMs; this.artworkUri=artworkUri;
    }

    public static TrackRecord from(Song song) {
        return new TrackRecord(Long.toString(song.id), song.uri.toString(), song.title,
                song.artist, song.album, song.duration, song.artworkUri==null?null:song.artworkUri.toString());
    }

    public Song toSong() {
        long id;
        try { id = Long.parseLong(mediaId); } catch (NumberFormatException ignored) { id = mediaId.hashCode(); }
        return new Song(id, title, artist, album, durationMs, Uri.parse(uri), artworkUri==null?null:Uri.parse(artworkUri));
    }
}
