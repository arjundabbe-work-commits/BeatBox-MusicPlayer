package com.beatbox.player;

import android.content.ComponentName;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.data.PlaylistEntity;
import com.beatbox.player.data.PlaylistTrackEntity;
import com.beatbox.player.data.TrackRecord;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public final class PlaylistDetailActivity extends AppCompatActivity {
    private BeatBoxApp app; private long playlistId; private PlaylistEntity playlist;
    private final List<PlaylistTrackEntity> tracks=new ArrayList<>(); private final List<String> labels=new ArrayList<>();
    private ArrayAdapter<String> adapter; private TextView title,empty; private MediaController controller; private ListenableFuture<MediaController> future;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_playlist_detail);app=(BeatBoxApp)getApplication();playlistId=getIntent().getLongExtra("playlist_id",-1);title=findViewById(R.id.playlist_title);empty=findViewById(R.id.empty_tracks);ListView list=findViewById(R.id.playlist_tracks);adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);findViewById(R.id.add_songs).setOnClickListener(v->chooseSongs());findViewById(R.id.play_playlist).setOnClickListener(v->play(0));list.setOnItemClickListener((p,v,pos,id)->play(pos));list.setOnItemLongClickListener((p,v,pos,id)->{actions(pos);return true;});}
    private void load(){app.io.execute(()->{PlaylistEntity found=app.database().playlists().get(playlistId);List<PlaylistTrackEntity> rows=app.database().playlists().getTracks(playlistId);runOnUiThread(()->{playlist=found;tracks.clear();tracks.addAll(rows);labels.clear();for(PlaylistTrackEntity t:rows)labels.add(t.title+"\n"+t.artist+"  •  "+t.album);adapter.notifyDataSetChanged();title.setText(found==null?"Playlist":found.name);empty.setVisibility(rows.isEmpty()?android.view.View.VISIBLE:android.view.View.GONE);});});}
    private void chooseSongs(){List<Song> library=app.songs();if(library.isEmpty()){android.widget.Toast.makeText(this,"Scan the music library first.",android.widget.Toast.LENGTH_SHORT).show();return;}String[] names=new String[library.size()];boolean[] checked=new boolean[library.size()];for(int i=0;i<library.size();i++)names[i]=library.get(i).title+" — "+library.get(i).artist;new AlertDialog.Builder(this).setTitle("Add songs").setMultiChoiceItems(names,checked,(d,w,is)->checked[w]=is).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->app.io.execute(()->{for(int i=0;i<library.size();i++)if(checked[i])app.database().playlists().append(playlistId,TrackRecord.from(library.get(i)));load();})).show();}
    private void actions(int position){new AlertDialog.Builder(this).setItems(new String[]{"Move up","Move down","Remove"},(d,which)->{PlaylistTrackEntity track=tracks.get(position);if(which==2)app.io.execute(()->{app.database().playlists().removeEntry(playlistId,track.entryId);load();});else {int target=which==0?position-1:position+1;if(target>=0&&target<tracks.size())app.io.execute(()->{app.database().playlists().reorder(playlistId,track.entryId,target);load();});}}).show();}
    private void play(int index){if(tracks.isEmpty())return;if(controller==null){android.widget.Toast.makeText(this,"Player is connecting.",android.widget.Toast.LENGTH_SHORT).show();return;}ArrayList<MediaItem> items=new ArrayList<>();for(PlaylistTrackEntity t:tracks){TrackRecord r=new TrackRecord(t.mediaId,t.uri,t.title,t.artist,t.album,t.durationMs,t.artworkUri);Song s=r.toSong();items.add(new MediaItem.Builder().setMediaId(t.mediaId).setUri(s.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).setArtworkUri(s.artworkUri).build()).build());}controller.setMediaItems(items,index,0);controller.prepare();controller.play();}
    @Override protected void onStart(){super.onStart();load();future=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();future.addListener(()->{if(!isFinishing()&&!isDestroyed())try{controller=future.get();}catch(Exception ignored){}},command->new android.os.Handler(android.os.Looper.getMainLooper()).post(command));}
    @Override protected void onStop(){if(future!=null){MediaController.releaseFuture(future);future=null;controller=null;}super.onStop();}
}
