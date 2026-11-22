package com.beatbox.player;

import android.content.ComponentName;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.data.PlayHistoryEntity;
import com.beatbox.player.data.TrackRecord;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

public final class RecentActivity extends AppCompatActivity {
    private BeatBoxApp app;private final List<Song> songs=new ArrayList<>();private ArrayAdapter<Song> adapter;private MediaController controller;private ListenableFuture<MediaController> future;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_recent);app=(BeatBoxApp)getApplication();ListView list=findViewById(R.id.recent_list);adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,songs);list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->play(pos));}
    private void load(){app.io.execute(()->{List<PlayHistoryEntity> rows=app.database().history().recent(100);List<Song> result=new ArrayList<>();for(PlayHistoryEntity e:rows)result.add(new TrackRecord(e.mediaId,e.uri,e.title,e.artist,e.album,e.durationMs,e.artworkUri).toSong());runOnUiThread(()->{songs.clear();songs.addAll(result);adapter.notifyDataSetChanged();});});}
    private void play(int index){if(controller==null)return;ArrayList<MediaItem> queue=new ArrayList<>();for(Song s:songs)queue.add(new MediaItem.Builder().setMediaId(Long.toString(s.id)).setUri(s.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album).setArtworkUri(s.artworkUri).build()).build());controller.setMediaItems(queue,index,0);controller.prepare();controller.play();}
    @Override protected void onStart(){super.onStart();load();future=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();future.addListener(()->{if(!isFinishing()&&!isDestroyed())try{controller=future.get();}catch(Exception ignored){}},command->new android.os.Handler(android.os.Looper.getMainLooper()).post(command));}
    @Override protected void onStop(){if(future!=null){MediaController.releaseFuture(future);future=null;controller=null;}super.onStop();}
}
