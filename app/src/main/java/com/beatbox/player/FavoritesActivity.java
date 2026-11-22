package com.beatbox.player;

import android.content.ComponentName;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.data.FavoriteEntity;
import com.beatbox.player.data.TrackRecord;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public final class FavoritesActivity extends AppCompatActivity {
    private BeatBoxApp app;
    private final List<Song> songs = new ArrayList<>();
    private ArrayAdapter<Song> adapter;
    private TextView empty;
    private MediaController controller;
    private ListenableFuture<MediaController> future;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_favorites); app=(BeatBoxApp)getApplication();
        empty=findViewById(R.id.empty); ListView list=findViewById(R.id.favorite_list);
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,songs); list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->play(pos));
        list.setOnItemLongClickListener((p,v,pos,id)->{Song song=songs.get(pos); app.io.execute(()->{app.database().favorites().remove(Long.toString(song.id)); load();}); return true;});
        findViewById(R.id.play_all).setOnClickListener(v->play(0));
    }
    private void load(){app.io.execute(()->{List<FavoriteEntity> rows=app.database().favorites().getAll();List<Song> found=new ArrayList<>();for(FavoriteEntity row:rows)found.add(new TrackRecord(row.mediaId,row.uri,row.title,row.artist,row.album,row.durationMs,row.artworkUri).toSong());runOnUiThread(()->{songs.clear();songs.addAll(found);adapter.notifyDataSetChanged();empty.setVisibility(songs.isEmpty()?android.view.View.VISIBLE:android.view.View.GONE);});});}
    private void play(int index){if(songs.isEmpty())return;if(controller==null){Toast.makeText(this,"Player is connecting.",Toast.LENGTH_SHORT).show();return;}ArrayList<MediaItem> items=new ArrayList<>();for(Song s:songs)items.add(new MediaItem.Builder().setMediaId(Long.toString(s.id)).setUri(s.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setAlbumTitle(s.album).setArtworkUri(s.artworkUri).build()).build());controller.setMediaItems(items,index,0);controller.prepare();controller.play();}
    @Override protected void onStart(){super.onStart();load();SessionToken token=new SessionToken(this,new ComponentName(this,PlaybackService.class));future=new MediaController.Builder(this,token).buildAsync();future.addListener(()->{if(!isFinishing()&&!isDestroyed())try{controller=future.get();}catch(Exception ignored){}},command->new android.os.Handler(android.os.Looper.getMainLooper()).post(command));}
    @Override protected void onStop(){if(future!=null){MediaController.releaseFuture(future);future=null;controller=null;}super.onStop();}
}
