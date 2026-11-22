package com.beatbox.player;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public final class SearchActivity extends AppCompatActivity {
    private static SearchActivity active;
    private final List<Song> results = new ArrayList<>();
    private ArrayAdapter<Song> adapter;
    private EditText query;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_search);
        query = findViewById(R.id.search_query); ListView list = findViewById(R.id.search_results);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, results); list.setAdapter(adapter);
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ update(s.toString()); }
            public void afterTextChanged(Editable e){}
        });
        list.setOnItemClickListener((p,v,pos,id) -> { HomeActivity home=HomeActivity.active; if(home!=null) home.playSong(results.get(pos)); finish(); });
    }
    private void update(String q) {
        results.clear(); HomeActivity home=HomeActivity.active;
        if(home!=null && !q.trim().isEmpty()) for(Song s:home.songs) if((s.title+" "+s.artist+" "+s.album).toLowerCase(java.util.Locale.ROOT).contains(q.toLowerCase(java.util.Locale.ROOT))) results.add(s);
        adapter.notifyDataSetChanged();
    }
    static void refreshActiveResults() { if (active != null && active.query != null) active.update(active.query.getText().toString()); }
    @Override protected void onStart() { super.onStart(); active = this; refreshActiveResults(); }
    @Override protected void onStop() { if (active == this) active = null; super.onStop(); }
}
