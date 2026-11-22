package com.beatbox.player;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    static HomeActivity active;
    final List<Song> songs = new ArrayList<>();
    final ExecutorService io = Executors.newSingleThreadExecutor();
    MediaController controller;
    ListenableFuture<MediaController> controllerFuture;
    ListView list;
    TextView status, miniTitle;
    ImageButton miniPlay;
    private final Player.Listener miniListener = new Player.Listener() {
        @Override public void onIsPlayingChanged(boolean playing) { refreshMini(); }
        @Override public void onMediaItemTransition(MediaItem item, int reason) { refreshMini(); SearchActivity.refreshActiveResults(); }
    };
    final ActivityResultLauncher<String> permission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) scan(); else status.setText("Music permission is needed to browse songs stored on this device.");
    });

    @Override protected void onCreate(Bundle state) {
        active = this;
        super.onCreate(state); setContentView(R.layout.activity_home);
        list = findViewById(R.id.song_list); status = findViewById(R.id.library_status);
        miniTitle = findViewById(R.id.mini_title); miniPlay = findViewById(R.id.mini_play);
        findViewById(R.id.search_button).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.mini_player).setOnClickListener(v -> startActivity(new Intent(this, NowPlayingActivity.class)));
        miniPlay.setOnClickListener(v -> { if (controller != null) { if (controller.isPlaying()) controller.pause(); else controller.play(); refreshMini(); } });
        list.setOnItemClickListener((parent, view, position, id) -> playFrom(position));
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            if (isFinishing() || isDestroyed()) return;
            try { controller = controllerFuture.get(); controller.addListener(miniListener); refreshMini(); }
            catch (Exception ignored) { status.setText("The playback service could not be started."); }
        }, command -> new Handler(Looper.getMainLooper()).post(command));
        if (hasAudioPermission()) scan(); else permission.launch(audioPermission());
    }
    private String audioPermission() { return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE; }
    private boolean hasAudioPermission() { return checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED; }
    private void scan() {
        status.setText("Scanning device music…");
        io.execute(() -> {
            List<Song> found = new ArrayList<>();
            Uri collection = Build.VERSION.SDK_INT >= 29 ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION};
            try (Cursor c = getContentResolver().query(collection, projection, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")) {
                if (c != null) while (c.moveToNext()) {
                    long id = c.getLong(0); String title = c.getString(1); String artist = c.getString(2); String album = c.getString(3);
                    found.add(new Song(id, title == null ? "Unknown title" : title, artist == null ? "Unknown artist" : artist, album == null ? "Unknown album" : album, c.getLong(4), Uri.withAppendedPath(collection, Long.toString(id))));
                }
            } catch (SecurityException error) {
                runOnUiThread(() -> { if (!isDestroyed()) status.setText("Music access was removed. Grant permission to scan your library."); });
                return;
            } catch (Exception error) {
                runOnUiThread(() -> { if (!isDestroyed()) status.setText("Unable to scan device music. Try again later."); });
                return;
            }
            runOnUiThread(() -> { if (!isDestroyed()) showSongs(found); });
        });
    }
    private void showSongs(List<Song> found) {
        songs.clear(); songs.addAll(found);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs));
        status.setText(songs.isEmpty() ? "No music found. Add audio files to your device and refresh." : "All songs  ·  " + songs.size());
        refreshSearchResults();
    }
    void playFrom(int index) {
        if (controller == null || songs.isEmpty()) { Toast.makeText(this, "Player is connecting. Try again in a moment.", Toast.LENGTH_SHORT).show(); return; }
        ArrayList<MediaItem> items = new ArrayList<>();
        for (Song s : songs) items.add(new MediaItem.Builder().setMediaId(Long.toString(s.id)).setUri(s.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album).build()).build());
        controller.setMediaItems(items, index, 0); controller.prepare(); controller.play(); refreshMini();
    }
    void playSong(Song song) { int index = songs.indexOf(song); if (index >= 0) playFrom(index); }
    void refreshMini() {
        if (controller != null && controller.getMediaItemCount() == 0) miniTitle.setText("Nothing playing");
        else if (controller != null && controller.getMediaMetadata().title != null) miniTitle.setText(controller.getMediaMetadata().title.toString());
        if (controller != null) miniPlay.setImageResource(controller.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }
    void refreshSearchResults() { SearchActivity.refreshActiveResults(); }
    @Override protected void onDestroy() {
        io.shutdown(); if (controller != null) controller.removeListener(miniListener); if (active == this) active = null;
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture); controller = null; super.onDestroy();
    }
}
