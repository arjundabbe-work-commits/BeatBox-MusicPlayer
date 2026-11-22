package com.beatbox.player;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import com.beatbox.player.data.PlaylistEntity;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public final class PlaylistsActivity extends AppCompatActivity {
    private BeatBoxApp app; private final List<PlaylistEntity> playlists=new ArrayList<>(); private final List<String> labels=new ArrayList<>(); private ArrayAdapter<String> adapter; private TextView empty;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_playlists);app=(BeatBoxApp)getApplication();ListView list=findViewById(R.id.playlists_list);empty=findViewById(R.id.empty_playlists);adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);findViewById(R.id.create_playlist).setOnClickListener(v->create());list.setOnItemClickListener((p,v,pos,id)->{android.content.Intent intent=new android.content.Intent(this,PlaylistDetailActivity.class);intent.putExtra("playlist_id",playlists.get(pos).id);startActivity(intent);});list.setOnItemLongClickListener((p,v,pos,id)->{edit(playlists.get(pos));return true;});}
    private void load(){app.io.execute(()->{List<PlaylistEntity> rows=app.database().playlists().getAll();runOnUiThread(()->{playlists.clear();playlists.addAll(rows);labels.clear();for(PlaylistEntity p:rows)labels.add(p.name);adapter.notifyDataSetChanged();empty.setVisibility(rows.isEmpty()?android.view.View.VISIBLE:android.view.View.GONE);});});}
    private EditText input(String initial){EditText field=new EditText(this);field.setSingleLine(true);field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);field.setHint("Playlist name");if(initial!=null)field.setText(initial);int pad=(int)(16*getResources().getDisplayMetrics().density);field.setPadding(pad,pad,pad,pad);return field;}
    private void create(){EditText name=input(null);new AlertDialog.Builder(this).setTitle("New playlist").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{String value=name.getText().toString().trim();if(value.isEmpty()){android.widget.Toast.makeText(this,"Enter a playlist name.",android.widget.Toast.LENGTH_SHORT).show();return;}app.io.execute(()->{long now=System.currentTimeMillis();app.database().playlists().create(new PlaylistEntity(value,now,now));load();});}).show();}
    private void edit(PlaylistEntity playlist){new AlertDialog.Builder(this).setTitle(playlist.name).setItems(new String[]{"Rename","Delete"},(d,which)->{if(which==0)rename(playlist);else new AlertDialog.Builder(this).setMessage("Delete this playlist?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->app.io.execute(()->{app.database().playlists().delete(playlist.id);load();})).show();}).show();}
    private void rename(PlaylistEntity playlist){EditText name=input(playlist.name);new AlertDialog.Builder(this).setTitle("Rename playlist").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String value=name.getText().toString().trim();if(!value.isEmpty())app.io.execute(()->{app.database().playlists().rename(playlist.id,value,System.currentTimeMillis());load();});}).show();}
    @Override protected void onStart(){super.onStart();load();}
}
