package com.beatbox.player;

import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.domain.MusicAlgorithms;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class LibraryActivity extends AppCompatActivity {
    private static final String[] CATEGORIES = {"All songs", "Albums", "Artists", "Folders", "Genres", "Recently added", "Recently played", "Favourites", "Playlists"};
    private BeatBoxApp app;
    private ListView itemsView;
    private TextView heading, message;
    private View trackActions;
    private String page = "categories";
    private final List<Song> tracks = new ArrayList<>();
    private final List<String> groupKeys = new ArrayList<>();
    private final Map<String, List<Song>> groups = new LinkedHashMap<>();
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_library);
        app = (BeatBoxApp) getApplication();
        itemsView = findViewById(R.id.library_items);
        heading = findViewById(R.id.library_heading);
        message = findViewById(R.id.library_message);
        trackActions = findViewById(R.id.library_track_actions);
        findViewById(R.id.library_back).setOnClickListener(v -> navigateBack());
        findViewById(R.id.library_play_all).setOnClickListener(v -> playWholeList(false));
        findViewById(R.id.library_shuffle).setOnClickListener(v -> playWholeList(true));
        findViewById(R.id.library_add_queue).setOnClickListener(v -> addListToQueue());
        itemsView.setOnItemClickListener((parent, view, position, id) -> onItemSelected(position));
        showCategories();
    }

    private void showCategories() {
        page = "categories";
        trackActions.setVisibility(View.GONE);
        groups.clear();
        groupKeys.clear();
        heading.setText("Your Library");
        message.setText("Browse music stored on this device");
        itemsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CATEGORIES));
    }

    private void onItemSelected(int position) {
        if (page.equals("categories")) {
            switch (CATEGORIES[position]) {
                case "All songs": showTracks("All songs", MusicAlgorithms.sort(app.songs(), MusicAlgorithms.Sort.TITLE)); break;
                case "Albums": groupSongs("Albums", song -> song.album + "  ·  " + song.artist); break;
                case "Artists": groupSongs("Artists", song -> song.artist); break;
                case "Folders": groupSongs("Folders", song -> song.folder); break;
                case "Recently added": {
                    List<Song> recent = new ArrayList<>(app.songs());
                    recent.sort(Comparator.comparingLong((Song song) -> song.dateAddedSeconds).reversed().thenComparing(song -> song.title.toLowerCase(Locale.ROOT)));
                    showTracks("Recently added", recent);
                    break;
                }
                case "Genres": loadGenres(); break;
                case "Recently played": startActivity(new Intent(this, RecentActivity.class)); break;
                case "Favourites": startActivity(new Intent(this, FavoritesActivity.class)); break;
                case "Playlists": startActivity(new Intent(this, PlaylistsActivity.class)); break;
                default: break;
            }
        } else if (page.equals("groups")) {
            if (position >= 0 && position < groupKeys.size()) showTracks(groupKeys.get(position), groups.get(groupKeys.get(position)));
        } else if (page.equals("tracks")) {
            playTracks(position);
        }
    }

    private interface GroupLabel { String label(Song song); }

    private void groupSongs(String title, GroupLabel labeler) {
        Map<String, List<Song>> sorted = com.beatbox.player.domain.LibraryAlgorithms.groupBy(app.songs(), labeler::label);
        groups.clear();
        groups.putAll(sorted);
        groupKeys.clear();
        groupKeys.addAll(groups.keySet());
        page = "groups";
        trackActions.setVisibility(View.GONE);
        heading.setText(title);
        message.setText(groupKeys.isEmpty() ? "No " + title.toLowerCase(Locale.ROOT) + " found" : Integer.toString(groupKeys.size()) + " " + title.toLowerCase(Locale.ROOT));
        List<String> rows = new ArrayList<>();
        for (String key : groupKeys) rows.add(key + "\n" + groups.get(key).size() + " tracks");
        itemsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
    }

    private void loadGenres() {
        page = "loading";
        trackActions.setVisibility(View.GONE);
        heading.setText("Genres");
        message.setText("Loading genre tags from MediaStore…");
        itemsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[0]));
        app.io.execute(() -> {
            Map<Long, Song> byId = new LinkedHashMap<>();
            for (Song song : app.songs()) byId.put(song.id, song);
            TreeMap<String, List<Song>> found = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            try (Cursor genres = getContentResolver().query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                    new String[]{BaseColumns._ID, MediaStore.Audio.Genres.NAME}, null, null, MediaStore.Audio.Genres.NAME + " COLLATE NOCASE ASC")) {
                if (genres != null) while (genres.moveToNext()) {
                    long genreId = genres.getLong(0);
                    String name = genres.getString(1);
                    if (name == null || name.trim().isEmpty()) name = "Unknown genre";
                    List<Song> members = new ArrayList<>();
                    try (Cursor rows = getContentResolver().query(MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                            new String[]{MediaStore.Audio.Genres.Members.AUDIO_ID}, null, null, null)) {
                        if (rows != null) while (rows.moveToNext()) {
                            Song song = byId.get(rows.getLong(0));
                            if (song != null) members.add(song);
                        }
                    }
                    if (!members.isEmpty()) found.put(name, members);
                }
            } catch (SecurityException | IllegalArgumentException ignored) { }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !page.equals("loading")) return;
                groups.clear(); groups.putAll(found); groupKeys.clear(); groupKeys.addAll(groups.keySet()); page = "groups";
                message.setText(groupKeys.isEmpty() ? "No genre metadata is available for these songs." : groupKeys.size() + " genres");
                List<String> rows = new ArrayList<>();
                for (String key : groupKeys) rows.add(key + "\n" + groups.get(key).size() + " tracks");
                itemsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
            });
        });
    }

    private void showTracks(String title, List<Song> songs) {
        tracks.clear();
        if (songs != null) tracks.addAll(songs);
        page = "tracks";
        trackActions.setVisibility(View.VISIBLE);
        heading.setText(title);
        message.setText(tracks.isEmpty() ? "No songs found" : tracks.size() + " tracks · Tap a song to play this list");
        List<String> rows = new ArrayList<>();
        for (Song song : tracks) rows.add(song.title + "\n" + song.artist + "  ·  " + song.album);
        itemsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
    }

    private void playTracks(int selected) {
        if (controller == null || selected < 0 || selected >= tracks.size()) return;
        List<MediaItem> queue = mediaItems();
        controller.setMediaItems(queue, selected, 0);
        controller.prepare();
        controller.play();
    }

    private List<MediaItem> mediaItems() {
        List<MediaItem> queue = new ArrayList<>();
        for (Song song : tracks) queue.add(new MediaItem.Builder().setMediaId(Long.toString(song.id)).setUri(song.uri)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist)
                        .setAlbumTitle(song.album).setArtworkUri(song.artworkUri).build()).build());
        return queue;
    }

    private void playWholeList(boolean shuffle) {
        if (tracks.isEmpty()) return;
        if (controller == null) { Toast.makeText(this, "Playback is connecting.", Toast.LENGTH_SHORT).show(); return; }
        controller.setMediaItems(mediaItems());
        controller.setShuffleModeEnabled(shuffle);
        controller.prepare();
        controller.play();
    }

    private void addListToQueue() {
        if (tracks.isEmpty()) return;
        if (controller == null) { Toast.makeText(this, "Playback is connecting.", Toast.LENGTH_SHORT).show(); return; }
        List<MediaItem> items = mediaItems();
        if (controller.getMediaItemCount() == 0) {
            controller.setMediaItems(items);
            controller.prepare();
            controller.play();
        } else controller.addMediaItems(items);
        Toast.makeText(this, "Added " + items.size() + " tracks to the queue.", Toast.LENGTH_SHORT).show();
    }

    private void navigateBack() {
        if (page.equals("tracks")) {
            if (groups.isEmpty()) showCategories();
            else {
                page = "groups";
                trackActions.setVisibility(View.GONE);
                heading.setText("Choose a collection");
                List<String> rows = new ArrayList<>();
                for (String key : groupKeys) rows.add(key + "\n" + groups.get(key).size() + " tracks");
                itemsView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
            }
        } else showCategories();
    }

    @Override public void onBackPressed() {
        if (!page.equals("categories")) navigateBack(); else super.onBackPressed();
    }

    @Override protected void onStart() {
        super.onStart();
        controllerFuture = new MediaController.Builder(this, new SessionToken(this, new ComponentName(this, PlaybackService.class))).buildAsync();
        controllerFuture.addListener(() -> {
            if (isFinishing() || isDestroyed()) return;
            try { controller = controllerFuture.get(); } catch (Exception ignored) { message.setText("Playback service unavailable"); }
        }, command -> new android.os.Handler(getMainLooper()).post(command));
    }

    @Override protected void onStop() {
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); controllerFuture = null; }
        controller = null;
        super.onStop();
    }
}
