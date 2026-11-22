package com.beatbox.player;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import com.beatbox.player.data.TrackRecord;
import com.beatbox.player.data.QueueTrackEntity;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public final class PlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession session;
    private BeatBoxApp app;
    private AudioEffectsManager audioEffects;
    private String historyRecordedFor;
    private final Player.Listener persistenceListener = new Player.Listener() {
        @Override public void onTimelineChanged(androidx.media3.common.Timeline timeline, int reason) { persistQueue(); }
        @Override public void onMediaItemTransition(MediaItem item, int reason) { historyRecordedFor=null; recordCurrentIfPlaying(); }
        @Override public void onIsPlayingChanged(boolean isPlaying) { if(isPlaying)recordCurrentIfPlaying(); }
    };

    @Override public void onCreate() {
        super.onCreate();
        app = (BeatBoxApp) getApplication();
        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        player.addListener(persistenceListener);
        audioEffects = new AudioEffectsManager(this, player);
        session = new MediaSession.Builder(this, player).build();
        restoreQueue();
    }
    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return session; }
    @Override public void onDestroy() {
        if (audioEffects != null) { audioEffects.release(); audioEffects = null; }
        if (session != null) { session.release(); session = null; }
        if (player != null) { player.removeListener(persistenceListener); player.release(); player = null; }
        super.onDestroy();
    }

    private void persistQueue() {
        if (player == null || app == null) return;
        List<TrackRecord> tracks = new ArrayList<>();
        for (int i=0;i<player.getMediaItemCount();i++) {
            MediaItem item=player.getMediaItemAt(i);
            if(item.localConfiguration==null || item.localConfiguration.uri==null) continue;
            String id=item.mediaId;
            CharSequence title=item.mediaMetadata.title;
            CharSequence artist=item.mediaMetadata.artist;
            CharSequence album=item.mediaMetadata.albumTitle;
            tracks.add(new TrackRecord(id,item.localConfiguration.uri.toString(),title==null?id:title.toString(),artist==null?"Unknown artist":artist.toString(),album==null?"Unknown album":album.toString(),0,item.mediaMetadata.artworkUri==null?null:item.mediaMetadata.artworkUri.toString()));
        }
        app.io.execute(()->app.database().queue().replace(tracks));
    }

    private void recordHistory(MediaItem item) {
        if(item==null || item.localConfiguration==null || item.localConfiguration.uri==null) return;
        String id=item.mediaId; CharSequence title=item.mediaMetadata.title; CharSequence artist=item.mediaMetadata.artist; CharSequence album=item.mediaMetadata.albumTitle;
        TrackRecord track=new TrackRecord(id,item.localConfiguration.uri.toString(),title==null?id:title.toString(),artist==null?"Unknown artist":artist.toString(),album==null?"Unknown album":album.toString(),0,item.mediaMetadata.artworkUri==null?null:item.mediaMetadata.artworkUri.toString());
        app.io.execute(()->app.database().history().record(track,System.currentTimeMillis()));
    }

    private void recordCurrentIfPlaying(){
        if(player==null||!player.isPlaying()||player.getCurrentMediaItem()==null)return;
        String id=player.getCurrentMediaItem().mediaId;
        if(id.equals(historyRecordedFor))return;
        historyRecordedFor=id;recordHistory(player.getCurrentMediaItem());
    }

    private void restoreQueue() {
        app.io.execute(()->{
            List<QueueTrackEntity> saved=app.database().queue().getAll();
            if(saved.isEmpty())return;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                if(player==null || player.getMediaItemCount()!=0)return;
                List<MediaItem> restored=new ArrayList<>();
                for(QueueTrackEntity row:saved){
                    android.net.Uri uri=android.net.Uri.parse(row.uri);
                    restored.add(new MediaItem.Builder().setMediaId(row.mediaId).setUri(uri).setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder().setTitle(row.title).setArtist(row.artist).setAlbumTitle(row.album).setArtworkUri(row.artworkUri==null?null:android.net.Uri.parse(row.artworkUri)).build()).build());
                }
                player.setMediaItems(restored);
            });
        });
    }
}
