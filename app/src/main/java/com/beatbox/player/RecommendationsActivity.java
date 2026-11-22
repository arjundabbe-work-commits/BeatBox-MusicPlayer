package com.beatbox.player;

import android.content.ComponentName;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.data.FavoriteEntity;
import com.beatbox.player.data.PlayHistoryEntity;
import com.beatbox.player.domain.RecommendationAlgorithms;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UnstableApi
public final class RecommendationsActivity extends AppCompatActivity {
    private final List<Song> recommendations = new ArrayList<>();
    private BeatBoxApp app;
    private ArrayAdapter<Song> adapter;
    private TextView message;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean started;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_recommendations);
        app = (BeatBoxApp) getApplication();
        message = findViewById(R.id.recommendation_message);
        ListView list = findViewById(R.id.recommendation_tracks);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recommendations);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> playFrom(position));
    }

    @Override protected void onStart() {
        super.onStart(); started = true;
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            if (!started || isFinishing() || isDestroyed()) return;
            try { controller = controllerFuture.get(); } catch (Exception ignored) { }
        }, command -> new android.os.Handler(getMainLooper()).post(command));
        loadRecommendations();
    }

    private void loadRecommendations() {
        List<Song> library = app.songs();
        app.io.execute(() -> {
            Map<Long, RecommendationAlgorithms.ListeningSignal> history = new HashMap<>();
            for (PlayHistoryEntity row : app.database().history().recent(500)) {
                try { history.put(Long.parseLong(row.mediaId), new RecommendationAlgorithms.ListeningSignal(row.playCount, row.lastPlayedAt, 0)); }
                catch (NumberFormatException ignored) { }
            }
            Set<Long> favorites = new HashSet<>();
            for (FavoriteEntity row : app.database().favorites().getAll()) {
                try { favorites.add(Long.parseLong(row.mediaId)); } catch (NumberFormatException ignored) { }
            }
            List<Song> picked = RecommendationAlgorithms.recommend(library, history, favorites, -1,
                    System.currentTimeMillis(), 100);
            runOnUiThread(() -> {
                if (!started || isFinishing() || isDestroyed()) return;
                recommendations.clear(); recommendations.addAll(picked); adapter.notifyDataSetChanged();
                message.setText(picked.isEmpty() ? "Add local songs to your library to get started." :
                        history.isEmpty() && favorites.isEmpty() ? "A fresh mix from music on this device · Tap a song to play" :
                                "Picked from your listening history and favourites · Tap a song to play");
            });
        });
    }

    private void playFrom(int index) {
        if (controller == null || index < 0 || index >= recommendations.size()) return;
        List<MediaItem> items = new ArrayList<>();
        for (Song song : recommendations) items.add(new MediaItem.Builder().setMediaId(Long.toString(song.id))
                .setUri(song.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(song.title)
                        .setArtist(song.artist).setAlbumTitle(song.album).setArtworkUri(song.artworkUri).build()).build());
        controller.setMediaItems(items, index, 0); controller.prepare(); controller.play();
    }

    @Override protected void onStop() {
        started = false;
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); controllerFuture = null; }
        controller = null;
        super.onStop();
    }
}
