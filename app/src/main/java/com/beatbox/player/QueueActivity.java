package com.beatbox.player;

import android.content.ComponentName;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

public final class QueueActivity extends AppCompatActivity {
    private MediaController controller; private ListenableFuture<MediaController> future; private final List<Integer> positions=new ArrayList<>(); private final List<String> labels=new ArrayList<>(); private ArrayAdapter<String> adapter; private TextView empty;
    private final Player.Listener listener=new Player.Listener(){@Override public void onTimelineChanged(androidx.media3.common.Timeline t,int r){render();}@Override public void onMediaItemTransition(MediaItem item,int reason){render();}};
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_queue);ListView list=findViewById(R.id.queue_list);empty=findViewById(R.id.empty_queue);adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->{if(controller!=null){controller.seekToDefaultPosition(positions.get(pos));controller.play();}});list.setOnItemLongClickListener((p,v,pos,id)->{actions(pos);return true;});findViewById(R.id.clear_queue).setOnClickListener(v->{if(controller!=null)controller.clearMediaItems();});}
    private void render(){if(controller==null)return;labels.clear();positions.clear();int current=controller.getCurrentMediaItemIndex();for(int i=0;i<controller.getMediaItemCount();i++){MediaItem item=controller.getMediaItemAt(i);CharSequence title=item.mediaMetadata.title;CharSequence artist=item.mediaMetadata.artist;labels.add((i==current?"▶  ":"")+(title==null?item.mediaId:title)+"\n"+(artist==null?"Unknown artist":artist));positions.add(i);}adapter.notifyDataSetChanged();empty.setVisibility(labels.isEmpty()?android.view.View.VISIBLE:android.view.View.GONE);}
    private void actions(int visiblePosition){int actual=positions.get(visiblePosition);new AlertDialog.Builder(this).setItems(new String[]{"Move up","Move down","Remove"},(d,which)->{if(controller==null)return;if(which==2)controller.removeMediaItem(actual);else{int target=which==0?actual-1:actual+1;if(target>=0&&target<controller.getMediaItemCount())controller.moveMediaItem(actual,target);}}).show();}
    @Override protected void onStart(){super.onStart();future=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();future.addListener(()->{if(!isFinishing()&&!isDestroyed())try{controller=future.get();controller.addListener(listener);render();}catch(Exception ignored){}},command->new android.os.Handler(android.os.Looper.getMainLooper()).post(command));}
    @Override protected void onStop(){if(controller!=null)controller.removeListener(listener);if(future!=null){MediaController.releaseFuture(future);future=null;controller=null;}super.onStop();}
}
