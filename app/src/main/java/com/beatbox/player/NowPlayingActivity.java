package com.beatbox.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.common.Player;
import com.google.common.util.concurrent.ListenableFuture;
import android.content.ComponentName;

public final class NowPlayingActivity extends AppCompatActivity {
    private HomeActivity home; private TextView title, artist; private ImageButton toggle; private SeekBar progress;
    private MediaController standaloneController;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean screenStarted;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (player() != null) {
                long duration = player().getDuration();
                if (duration > 0 && duration < Integer.MAX_VALUE) { progress.setMax((int) duration); progress.setProgress((int) Math.max(0, player().getCurrentPosition())); }
            }
            handler.postDelayed(this, 500);
        }
    };
    private final Player.Listener listener = new Player.Listener() {
        @Override public void onMediaItemTransition(androidx.media3.common.MediaItem item,int reason){ refresh(); }
        @Override public void onIsPlayingChanged(boolean playing){ refresh(); }
    };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_now_playing);
        home=HomeActivity.active; title=findViewById(R.id.song_title); artist=findViewById(R.id.song_artist); toggle=findViewById(R.id.play_pause); progress=findViewById(R.id.progress);
        findViewById(R.id.previous).setOnClickListener(v->{if(player()!=null)player().seekToPreviousMediaItem();});
        findViewById(R.id.next).setOnClickListener(v->{if(player()!=null)player().seekToNextMediaItem();});
        toggle.setOnClickListener(v->{if(player()!=null){if(player().isPlaying())player().pause();else player().play();refresh();}});
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){} public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){if(player()!=null)player().seekTo((long)b.getProgress());}});
    }
    private MediaController player(){return standaloneController != null ? standaloneController : home == null ? null : home.controller;}
    private void refresh(){ if(player()==null)return; if(player().getMediaMetadata().title!=null)title.setText(player().getMediaMetadata().title); if(player().getMediaMetadata().artist!=null)artist.setText(player().getMediaMetadata().artist); toggle.setImageResource(player().isPlaying()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play); }
    @Override protected void onStart(){
        super.onStart(); screenStarted = true; home=HomeActivity.active;
        if (standaloneController != null) { standaloneController.addListener(listener); refresh(); }
        else {
            refresh();
            SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
            controllerFuture = new MediaController.Builder(this, token).buildAsync();
            controllerFuture.addListener(() -> {
                if (!screenStarted || isFinishing() || isDestroyed()) return;
                try { standaloneController = controllerFuture.get(); standaloneController.addListener(listener); refresh(); }
                catch (Exception ignored) { title.setText("Playback controls are unavailable"); }
            }, command -> new Handler(Looper.getMainLooper()).post(command));
        }
        handler.post(updateProgress);
    }
    @Override protected void onStop(){
        screenStarted = false; handler.removeCallbacks(updateProgress); if(player()!=null)player().removeListener(listener);
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); controllerFuture = null; standaloneController = null; }
        super.onStop();
    }
}
