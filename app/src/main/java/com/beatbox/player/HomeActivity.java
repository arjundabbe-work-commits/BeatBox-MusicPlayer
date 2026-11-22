package com.beatbox.player;

import android.Manifest;
import android.content.ActivityNotFoundException;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.domain.MusicAlgorithms;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    private BeatBoxApp app;
    final List<Song> songs = new ArrayList<>();
    final ExecutorService io = Executors.newSingleThreadExecutor();
    MediaController controller;
    ListenableFuture<MediaController> controllerFuture;
    ListView list;
    TextView status, miniTitle;
    ImageButton miniPlay;
    private boolean permissionDenied;
    private final Player.Listener miniListener = new Player.Listener() {
        @Override public void onIsPlayingChanged(boolean playing) { refreshMini(); }
        @Override public void onMediaItemTransition(MediaItem item, int reason) { refreshMini(); }
    };
    final ActivityResultLauncher<String> permission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) { permissionDenied=false; scan(); }
        else { permissionDenied=true; status.setText("Music permission is needed to browse songs stored on this device."); showDeniedPermissionHelp(); }
    });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_home);
        app = (BeatBoxApp) getApplication();
        list = findViewById(R.id.song_list); status = findViewById(R.id.library_status);
        miniTitle = findViewById(R.id.mini_title); miniPlay = findViewById(R.id.mini_play);
        findViewById(R.id.search_button).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.library_button).setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));
        findViewById(R.id.favorites_button).setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));
        findViewById(R.id.playlists_button).setOnClickListener(v -> startActivity(new Intent(this, PlaylistsActivity.class)));
        findViewById(R.id.recent_button).setOnClickListener(v -> startActivity(new Intent(this, RecentActivity.class)));
        findViewById(R.id.queue_button).setOnClickListener(v -> startActivity(new Intent(this, QueueActivity.class)));
        findViewById(R.id.settings_button).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.refresh_button).setOnClickListener(v -> { if (hasAudioPermission()) scan(); else explainAndRequestPermission(); });
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
        if (hasAudioPermission()) scan(); else explainAndRequestPermission();
    }
    private String audioPermission() { return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE; }
    private boolean hasAudioPermission() { return checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED; }
    private void explainAndRequestPermission(){
        if(hasAudioPermission()){scan();return;}
        new AlertDialog.Builder(this).setTitle("Find music on this device").setMessage("BeatBox reads your device's audio library so you can browse and play your local songs. Your music stays on this device.").setNegativeButton("Not now",(d,w)->status.setText("Allow music access and tap Refresh to scan your songs.")).setPositiveButton("Continue",(d,w)->{getSharedPreferences("beatbox_settings",MODE_PRIVATE).edit().putBoolean("audio_permission_requested",true).apply();permission.launch(audioPermission());}).show();
    }
    private void showDeniedPermissionHelp(){
        boolean asked=getSharedPreferences("beatbox_settings",MODE_PRIVATE).getBoolean("audio_permission_requested",false);
        if(!asked||shouldShowRequestPermissionRationale(audioPermission()))return;
        new AlertDialog.Builder(this).setTitle("Allow music access").setMessage("Android has blocked the permission prompt. Open BeatBox's app settings and allow music and audio access.").setNegativeButton("Later",null).setPositiveButton("Open settings",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException ignored){}}).show();
    }
    private void scan() {
        status.setText("Scanning device music…");
        io.execute(() -> {
            List<Song> found = new ArrayList<>();
            Uri collection = Build.VERSION.SDK_INT >= 29 ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String folderColumn = Build.VERSION.SDK_INT >= 29 ? MediaStore.Audio.Media.RELATIVE_PATH : MediaStore.Audio.Media.DATA;
            String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATE_ADDED, folderColumn};
            try (Cursor c = getContentResolver().query(collection, projection, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")) {
                if (c != null) while (c.moveToNext()) {
                    long id = c.getLong(0); String title = c.getString(1); String artist = c.getString(2); String album = c.getString(3);
                    long albumId=c.getLong(5); Uri artwork=albumId<0?null:Uri.withAppendedPath(Uri.parse("content://media/external/audio/albumart"),Long.toString(albumId));
                    String location = c.getString(7);
                    String folder = Build.VERSION.SDK_INT >= 29 ? trimFolder(location) : parentFolder(location);
                    found.add(new Song(id, title == null ? "Unknown title" : title, artist == null ? "Unknown artist" : artist, album == null ? "Unknown album" : album, c.getLong(4), Uri.withAppendedPath(collection, Long.toString(id)),artwork,c.getLong(6),folder));
                }
            } catch (SecurityException error) {
                runOnUiThread(() -> { if (!isDestroyed()) status.setText("Music access was removed. Grant permission to scan your library."); });
                return;
            } catch (Exception error) {
                runOnUiThread(() -> { if (!isDestroyed()) status.setText("Unable to scan device music. Try again later."); });
                return;
            }
            app.updateLibrary(found);
            String sort=app.database().settings().get("song_sort");
            runOnUiThread(() -> { if (!isDestroyed()) showSongs(found,sort); });
        });
    }
    private static String trimFolder(String path) {
        if (path == null || path.trim().isEmpty()) return "Unknown folder";
        String normalized = path.replace('\\', '/');
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized.isEmpty() ? "Unknown folder" : normalized;
    }
    private static String parentFolder(String path) {
        if (path == null || path.trim().isEmpty()) return "Unknown folder";
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return separator <= 0 ? "Unknown folder" : path.substring(0, separator);
    }
    private void showSongs(List<Song> found,String persistedSort) {
        String sort=persistedSort==null?getSharedPreferences("beatbox_settings",MODE_PRIVATE).getString("song_sort","TITLE"):persistedSort;
        MusicAlgorithms.Sort mode=sort.equals("ARTIST")?MusicAlgorithms.Sort.ARTIST:sort.equals("ALBUM")?MusicAlgorithms.Sort.ALBUM:sort.equals("DURATION")?MusicAlgorithms.Sort.DURATION:MusicAlgorithms.Sort.TITLE;
        songs.clear();songs.addAll(MusicAlgorithms.sort(found,mode));
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songs));
        status.setText(songs.isEmpty() ? "No music found. Add audio files to your device and refresh." : "All songs  ·  " + songs.size());
    }
    void playFrom(int index) {
        if (controller == null || songs.isEmpty()) { Toast.makeText(this, "Player is connecting. Try again in a moment.", Toast.LENGTH_SHORT).show(); return; }
        ArrayList<MediaItem> items = new ArrayList<>();
        for (Song s : songs) items.add(new MediaItem.Builder().setMediaId(Long.toString(s.id)).setUri(s.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album).setArtworkUri(s.artworkUri).build()).build());
        controller.setMediaItems(items, index, 0); controller.prepare(); controller.play(); refreshMini();
    }
    void refreshMini() {
        if (controller != null && controller.getMediaItemCount() == 0) miniTitle.setText("Nothing playing");
        else if (controller != null && controller.getMediaMetadata().title != null) miniTitle.setText(controller.getMediaMetadata().title.toString());
        if (controller != null) miniPlay.setImageResource(controller.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }
    @Override protected void onDestroy() {
        io.shutdown(); if (controller != null) controller.removeListener(miniListener);
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture); controller = null; super.onDestroy();
    }
    @Override protected void onResume(){super.onResume();if(permissionDenied&&hasAudioPermission()){permissionDenied=false;scan();}}
}
