package com.beatbox.player;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.domain.LrcParser;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LyricsActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<TextView> lineViews = new ArrayList<>();
    private final List<LrcParser.Line> lyricLines = new ArrayList<>();
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private ActivityResultLauncher<String[]> importLauncher;
    private TextView title, artist, message;
    private LinearLayout linesContainer;
    private ScrollView scroll;
    private String currentMediaId;
    private String importingForMediaId;
    private int highlightedLine = -2;
    private boolean screenStarted;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override public void onMediaItemTransition(MediaItem item, int reason) { refreshTrack(); }
        @Override public void onIsPlayingChanged(boolean isPlaying) { handler.removeCallbacks(updateHighlight); if (isPlaying) handler.post(updateHighlight); }
    };
    private final Runnable updateHighlight = new Runnable() {
        @Override public void run() {
            updateCurrentLine();
            if (controller != null && controller.isPlaying()) handler.postDelayed(this, 250);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_lyrics);
        title = findViewById(R.id.lyrics_title);
        artist = findViewById(R.id.lyrics_artist);
        message = findViewById(R.id.lyrics_message);
        linesContainer = findViewById(R.id.lyrics_lines);
        scroll = findViewById(R.id.lyrics_scroll);
        findViewById(R.id.lyrics_back).setOnClickListener(v -> finish());
        findViewById(R.id.import_lyrics).setOnClickListener(v -> {
            if (currentMediaId == null) {
                showMessage("Play a local song before importing lyrics.");
            } else {
                importingForMediaId = currentMediaId;
                importLauncher.launch(new String[]{"text/plain", "application/x-lrc", "text/*"});
            }
        });
        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onLyricsSelected);
    }

    private void onLyricsSelected(Uri uri) {
        if (uri == null || importingForMediaId == null) return;
        String mediaId = importingForMediaId;
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException ignored) { }
        getSharedPreferences("beatbox_lyrics", MODE_PRIVATE).edit().putString(keyFor(mediaId), uri.toString()).apply();
        currentMediaId = null;
        refreshTrack();
    }

    private void refreshTrack() {
        MediaController player = controller;
        MediaItem item = player == null ? null : player.getCurrentMediaItem();
        if (item == null || item.localConfiguration == null) {
            currentMediaId = null;
            title.setText("Lyrics");
            artist.setText("");
            showMessage("No song is playing. Start a local track, then import its lyrics.");
            return;
        }
        String id = item.mediaId;
        title.setText(item.mediaMetadata.title == null ? "Lyrics" : item.mediaMetadata.title);
        artist.setText(item.mediaMetadata.artist == null ? "Unknown artist" : item.mediaMetadata.artist);
        if (id.equals(currentMediaId)) { updateCurrentLine(); return; }
        currentMediaId = id;
        highlightedLine = -2;
        String saved = getSharedPreferences("beatbox_lyrics", MODE_PRIVATE).getString(keyFor(id), null);
        if (saved == null) {
            lyricLines.clear();
            showMessage("No local lyrics imported for this song.");
            return;
        }
        Uri uri = Uri.parse(saved);
        showMessage("Loading local lyrics…");
        io.execute(() -> {
            String content = readText(uri);
            List<LrcParser.Line> parsed = LrcParser.parse(content);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !id.equals(currentMediaId)) return;
                lyricLines.clear(); lyricLines.addAll(parsed);
                renderLyrics(parsed);
                updateCurrentLine();
            });
        });
    }

    private String readText(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) return null;
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                if (output.size() > 1_000_000) return null;
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) { return null; }
    }

    private void renderLyrics(List<LrcParser.Line> parsed) {
        linesContainer.removeAllViews();
        lineViews.clear();
        if (parsed.isEmpty()) {
            showMessage("Lyrics could not be read. Choose a plain text or LRC file.");
            return;
        }
        for (LrcParser.Line line : parsed) {
            TextView view = new TextView(this);
            view.setText(line.text);
            view.setTextSize(19);
            view.setTextColor(0xFFDDDDDD);
            view.setGravity(Gravity.CENTER_HORIZONTAL);
            view.setPadding(12, 12, 12, 12);
            linesContainer.addView(view, new LinearLayout.LayoutParams(-1, -2));
            lineViews.add(view);
        }
        boolean synchronizedLyrics = false;
        for (LrcParser.Line line : parsed) if (line.timeMs >= 0) { synchronizedLyrics = true; break; }
        message.setText(synchronizedLyrics ? "Local lyrics · timing follows playback" : "Local lyrics · unsynchronised text");
    }

    private void updateCurrentLine() {
        if (controller == null || lyricLines.isEmpty()) return;
        long position = controller.getCurrentPosition();
        int active = -1;
        for (int index = 0; index < lyricLines.size(); index++) {
            long time = lyricLines.get(index).timeMs;
            if (time >= 0 && time <= position) active = index;
        }
        if (active == highlightedLine) return;
        if (highlightedLine >= 0 && highlightedLine < lineViews.size()) {
            lineViews.get(highlightedLine).setTextColor(0xFFDDDDDD);
            lineViews.get(highlightedLine).setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        highlightedLine = active;
        if (active >= 0 && active < lineViews.size()) {
            TextView line = lineViews.get(active);
            line.setTextColor(0xFFFFFFFF);
            line.setTypeface(null, android.graphics.Typeface.BOLD);
            scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, line.getTop() - scroll.getHeight() / 3)));
        }
    }

    private void showMessage(String text) {
        lyricLines.clear();
        lineViews.clear();
        linesContainer.removeAllViews();
        TextView empty = new TextView(this);
        empty.setText(text);
        empty.setTextColor(0xFFDDDDDD);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(16, 48, 16, 48);
        linesContainer.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        message.setText("");
        highlightedLine = -2;
    }

    private static String keyFor(String mediaId) { return "track:" + mediaId; }

    @Override protected void onStart() {
        super.onStart();
        screenStarted = true;
        controllerFuture = new MediaController.Builder(this, new SessionToken(this, new ComponentName(this, PlaybackService.class))).buildAsync();
        controllerFuture.addListener(() -> {
            if (!screenStarted || isFinishing() || isDestroyed()) return;
            try {
                controller = controllerFuture.get();
                controller.addListener(playerListener);
                refreshTrack();
                if (controller.isPlaying()) handler.post(updateHighlight);
            } catch (Exception ignored) { showMessage("Playback service is unavailable."); }
        }, command -> new Handler(getMainLooper()).post(command));
    }

    @Override protected void onStop() {
        screenStarted = false;
        handler.removeCallbacks(updateHighlight);
        if (controller != null) controller.removeListener(playerListener);
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); controllerFuture = null; }
        controller = null;
        super.onStop();
    }

    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
