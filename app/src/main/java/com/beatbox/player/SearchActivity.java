package com.beatbox.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.domain.MusicAlgorithms;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public final class SearchActivity extends AppCompatActivity {
    private final List<Song> results = new ArrayList<>();
    private ArrayAdapter<Song> adapter;
    private EditText query;
    private BeatBoxApp app;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean screenStarted;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable pendingSearch=()->update(query==null?"":query.getText().toString());
    private final BeatBoxApp.LibraryListener libraryListener = songs -> update(query == null ? "" : query.getText().toString());
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_search); app=(BeatBoxApp)getApplication();
        query = findViewById(R.id.search_query); ListView list = findViewById(R.id.search_results);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, results); list.setAdapter(adapter);
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ handler.removeCallbacks(pendingSearch);handler.postDelayed(pendingSearch,180); }
            public void afterTextChanged(Editable e){}
        });
        list.setOnItemClickListener((p,v,pos,id) -> playSelected(results.get(pos)));
    }
    private void update(String q) {
        results.clear(); results.addAll(MusicAlgorithms.search(app.songs(),q));
        adapter.notifyDataSetChanged();
    }
    private void playSelected(Song song) {
        if (controller == null) { android.widget.Toast.makeText(this,"Player is connecting. Try again.",android.widget.Toast.LENGTH_SHORT).show(); return; }
        List<Song> library = app.songs();
        int index = library.indexOf(song);
        if (index < 0) return;
        java.util.ArrayList<MediaItem> items = new java.util.ArrayList<>();
        for (Song item : library) items.add(new MediaItem.Builder().setMediaId(Long.toString(item.id)).setUri(item.uri).setMediaMetadata(new MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist).setAlbumTitle(item.album).setArtworkUri(item.artworkUri).build()).build());
        controller.setMediaItems(items,index,0); controller.prepare(); controller.play(); finish();
    }
    @Override protected void onStart() {
        super.onStart(); screenStarted=true;app.addLibraryListener(libraryListener);
        if (controller == null && controllerFuture == null) {
            SessionToken token = new SessionToken(this,new android.content.ComponentName(this,PlaybackService.class));
            controllerFuture = new MediaController.Builder(this,token).buildAsync();
            controllerFuture.addListener(() -> {
                if (!screenStarted || isFinishing() || isDestroyed()) return;
                try { controller=controllerFuture.get(); }
                catch (Exception ignored) { android.widget.Toast.makeText(this,"Playback service is unavailable.",android.widget.Toast.LENGTH_SHORT).show(); }
            }, command -> new android.os.Handler(android.os.Looper.getMainLooper()).post(command));
        }
    }
    @Override protected void onStop() {
        screenStarted=false;handler.removeCallbacks(pendingSearch);
        app.removeLibraryListener(libraryListener);
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); controllerFuture=null; controller=null; }
        super.onStop();
    }
}
