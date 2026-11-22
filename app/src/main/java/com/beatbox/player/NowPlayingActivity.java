package com.beatbox.player;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.data.FavoriteEntity;
import com.beatbox.player.data.TrackRecord;
import com.beatbox.player.domain.MusicAlgorithms;
import com.beatbox.player.domain.RadioAlgorithms;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NowPlayingActivity extends AppCompatActivity {
    private BeatBoxApp app;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean screenStarted;
    private boolean scrubbing;
    private String requestedArtwork;
    private TextView title, artist, album, currentTime, totalTime;
    private ImageView cover;
    private ImageButton toggle, shuffle, repeat, favorite;
    private Button speedButton;
    private SeekBar progress;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService artworkExecutor = Executors.newSingleThreadExecutor();
    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            MediaController player=player();
            if(player!=null){
                long duration=player.getDuration(), position=player.getCurrentPosition();
                if(duration>0&&duration<Integer.MAX_VALUE){progress.setMax((int)duration);if(!scrubbing)progress.setProgress((int)Math.max(0,position));}
                currentTime.setText(MusicAlgorithms.formatTime(position));totalTime.setText(MusicAlgorithms.formatTime(duration));
            }
            handler.postDelayed(this,500);
        }
    };
    private final Player.Listener listener = new Player.Listener() {
        @Override public void onMediaItemTransition(MediaItem item,int reason){refresh();}
        @Override public void onIsPlayingChanged(boolean playing){refreshControls();}
        @Override public void onShuffleModeEnabledChanged(boolean enabled){refreshControls();}
        @Override public void onRepeatModeChanged(int mode){refreshControls();}
        @Override public void onPlaybackParametersChanged(PlaybackParameters parameters){refreshControls();}
        @Override public void onPlaybackStateChanged(int state){refresh();}
    };

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_now_playing);app=(BeatBoxApp)getApplication();
        title=findViewById(R.id.song_title);artist=findViewById(R.id.song_artist);album=findViewById(R.id.song_album);cover=findViewById(R.id.cover);
        currentTime=findViewById(R.id.current_time);totalTime=findViewById(R.id.total_time);progress=findViewById(R.id.progress);
        toggle=findViewById(R.id.play_pause);shuffle=findViewById(R.id.shuffle);repeat=findViewById(R.id.repeat);favorite=findViewById(R.id.favorite);
        speedButton=findViewById(R.id.playback_speed);
        findViewById(R.id.previous).setOnClickListener(v->{if(player()!=null)player().seekToPreviousMediaItem();});
        findViewById(R.id.next).setOnClickListener(v->{if(player()!=null)player().seekToNextMediaItem();});
        toggle.setOnClickListener(v->{if(player()!=null){if(player().isPlaying())player().pause();else player().play();refreshControls();}});
        shuffle.setOnClickListener(v->{if(player()!=null)player().setShuffleModeEnabled(!player().getShuffleModeEnabled());});
        repeat.setOnClickListener(v->{if(player()!=null){int mode=player().getRepeatMode();player().setRepeatMode(mode==Player.REPEAT_MODE_OFF?Player.REPEAT_MODE_ALL:mode==Player.REPEAT_MODE_ALL?Player.REPEAT_MODE_ONE:Player.REPEAT_MODE_OFF);}});
        favorite.setOnClickListener(v->toggleFavorite());
        findViewById(R.id.open_queue).setOnClickListener(v->startActivity(new android.content.Intent(this,QueueActivity.class)));
        findViewById(R.id.open_lyrics).setOnClickListener(v->startActivity(new android.content.Intent(this,LyricsActivity.class)));
        findViewById(R.id.start_radio).setOnClickListener(v->startLocalRadio());
        speedButton.setOnClickListener(v->choosePlaybackSpeed());
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int value,boolean fromUser){}public void onStartTrackingTouch(SeekBar b){scrubbing=true;}public void onStopTrackingTouch(SeekBar b){if(player()!=null)player().seekTo((long)b.getProgress());scrubbing=false;}});
    }

    private MediaController player(){return controller;}
    private void refresh(){
        MediaController p=player(); if(p==null)return;
        MediaItem item=p.getCurrentMediaItem();
        if(item==null){title.setText("Select a song");artist.setText("");album.setText("");cover.setImageResource(R.drawable.artwork_placeholder);requestedArtwork=null;refreshControls();return;}
        MediaMetadata metadata=item.mediaMetadata;
        title.setText(metadata.title==null?item.mediaId:metadata.title);
        artist.setText(metadata.artist==null?"Unknown artist":metadata.artist);
        album.setText(metadata.albumTitle==null?"":metadata.albumTitle);
        loadArtwork(metadata.artworkUri);
        refreshFavorite(item);
        refreshControls();
    }
    private void refreshControls(){
        MediaController p=player();if(p==null)return;
        toggle.setImageResource(p.isPlaying()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
        shuffle.setAlpha(p.getShuffleModeEnabled()?1f:0.55f);
        int repeatMode=p.getRepeatMode();repeat.setAlpha(repeatMode==Player.REPEAT_MODE_OFF?0.55f:1f);
        repeat.setContentDescription(repeatMode==Player.REPEAT_MODE_ONE?"Repeat one":repeatMode==Player.REPEAT_MODE_ALL?"Repeat all":"Repeat off");
        speedButton.setText(String.format(java.util.Locale.ROOT,"Speed %.2g×",p.getPlaybackParameters().speed));
    }
    private void choosePlaybackSpeed(){
        MediaController p=player();if(p==null)return;
        float[] speeds={0.75f,1f,1.25f,1.5f,1.75f,2f};
        String[] labels={"0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
        int selected=1;for(int i=0;i<speeds.length;i++)if(Math.abs(p.getPlaybackParameters().speed-speeds[i])<0.02f)selected=i;
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(labels,selected,(dialog,which)->{
            float speed=speeds[which];MediaController current=player();
            if(current!=null)current.setPlaybackParameters(new PlaybackParameters(speed));
            getSharedPreferences("beatbox_settings",MODE_PRIVATE).edit().putFloat("playback_speed",speed).apply();
            app.io.execute(()->app.database().settings().set("playback_speed",Float.toString(speed)));
            dialog.dismiss();
        }).setNegativeButton("Close",null).show();
    }
    private void toggleFavorite(){
        MediaController p=player();MediaItem item=p==null?null:p.getCurrentMediaItem();
        if(item==null||item.localConfiguration==null){android.widget.Toast.makeText(this,"Play a local song first.",android.widget.Toast.LENGTH_SHORT).show();return;}
        MediaMetadata m=item.mediaMetadata;String id=item.mediaId;
        TrackRecord record=new TrackRecord(id,item.localConfiguration.uri.toString(),m.title==null?id:m.title.toString(),m.artist==null?"Unknown artist":m.artist.toString(),m.albumTitle==null?"Unknown album":m.albumTitle.toString(),p.getDuration(),m.artworkUri==null?null:m.artworkUri.toString());
        app.io.execute(()->{boolean saved=app.database().favorites().toggle(new FavoriteEntity(record,System.currentTimeMillis()));runOnUiThread(()->favorite.setImageResource(saved?android.R.drawable.btn_star_big_on:android.R.drawable.btn_star_big_off));});
    }
    private void startLocalRadio(){
        MediaController p=player();MediaItem current=p==null?null:p.getCurrentMediaItem();
        if(current==null){android.widget.Toast.makeText(this,"Play a local song to start radio.",android.widget.Toast.LENGTH_SHORT).show();return;}
        Song seed=null;for(Song song:app.songs())if(Long.toString(song.id).equals(current.mediaId)){seed=song;break;}
        if(seed==null){android.widget.Toast.makeText(this,"Refresh your library before starting radio.",android.widget.Toast.LENGTH_SHORT).show();return;}
        java.util.List<Song> station=RadioAlgorithms.fromSeed(app.songs(),seed,30);
        if(station.size()<2){android.widget.Toast.makeText(this,"Add more local music to build a radio queue.",android.widget.Toast.LENGTH_SHORT).show();return;}
        java.util.ArrayList<MediaItem> queue=new java.util.ArrayList<>();
        for(Song song:station)queue.add(new MediaItem.Builder().setMediaId(Long.toString(song.id)).setUri(song.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album).setArtworkUri(song.artworkUri).build()).build());
        p.setShuffleModeEnabled(false);p.setMediaItems(queue);p.setRepeatMode(Player.REPEAT_MODE_OFF);p.prepare();p.play();
        android.widget.Toast.makeText(this,"Local radio started from " + seed.artist + ".",android.widget.Toast.LENGTH_SHORT).show();
    }
    private void refreshFavorite(MediaItem item){
        if(item==null)return;String id=item.mediaId;
        app.io.execute(()->{boolean saved=app.database().favorites().contains(id);runOnUiThread(()->{if(player()!=null&&player().getCurrentMediaItem()!=null&&id.equals(player().getCurrentMediaItem().mediaId))favorite.setImageResource(saved?android.R.drawable.btn_star_big_on:android.R.drawable.btn_star_big_off);});});
    }
    private void loadArtwork(Uri uri){
        if(uri==null){if(requestedArtwork==null)return;requestedArtwork=null;cover.setImageResource(R.drawable.artwork_placeholder);return;}
        String value=uri.toString();if(value.equals(requestedArtwork))return;requestedArtwork=value;cover.setImageResource(R.drawable.artwork_placeholder);
        artworkExecutor.execute(()->{
            Bitmap bitmap=null;
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            try(InputStream input=getContentResolver().openInputStream(uri)){if(input!=null)BitmapFactory.decodeStream(input,null,bounds);}catch(Exception ignored){}
            if(bounds.outWidth>0&&bounds.outHeight>0){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
                while(bounds.outWidth/options.inSampleSize>640||bounds.outHeight/options.inSampleSize>640)options.inSampleSize*=2;
                try(InputStream input=getContentResolver().openInputStream(uri)){if(input!=null)bitmap=BitmapFactory.decodeStream(input,null,options);}catch(Exception ignored){}
            }
            Bitmap result=bitmap;runOnUiThread(()->{if(value.equals(requestedArtwork)&&result!=null)cover.setImageBitmap(result);});
        });
    }
    @Override protected void onStart(){super.onStart();screenStarted=true;controllerFuture=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();controllerFuture.addListener(()->{if(!screenStarted||isFinishing()||isDestroyed())return;try{controller=controllerFuture.get();controller.addListener(listener);float speed=getSharedPreferences("beatbox_settings",MODE_PRIVATE).getFloat("playback_speed",1f);if(Math.abs(controller.getPlaybackParameters().speed-speed)>0.02f)controller.setPlaybackParameters(new PlaybackParameters(speed));refresh();}catch(Exception ignored){title.setText("Playback controls are unavailable");}},command->new Handler(Looper.getMainLooper()).post(command));handler.post(updateProgress);}
    @Override protected void onStop(){screenStarted=false;handler.removeCallbacks(updateProgress);if(controller!=null)controller.removeListener(listener);if(controllerFuture!=null){MediaController.releaseFuture(controllerFuture);controllerFuture=null;controller=null;}super.onStop();}
    @Override protected void onDestroy(){artworkExecutor.shutdownNow();super.onDestroy();}
}
