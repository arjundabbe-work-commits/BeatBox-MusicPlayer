package com.beatbox.player;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public final class SettingsActivity extends AppCompatActivity {
    private BeatBoxApp app;
    private final String[] sorts={"Title","Artist","Album","Duration"};
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_settings);app=(BeatBoxApp)getApplication();findViewById(R.id.sort_order).setOnClickListener(v->chooseSort());findViewById(R.id.clear_history).setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Clear recently played history?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->app.io.execute(()->app.database().history().clear())).show());findViewById(R.id.open_about).setOnClickListener(v->startActivity(new android.content.Intent(this,AboutUsActivity.class)));}
    private void chooseSort(){String selected=getSharedPreferences("beatbox_settings",MODE_PRIVATE).getString("song_sort","TITLE");int checked=selected.equals("ARTIST")?1:selected.equals("ALBUM")?2:selected.equals("DURATION")?3:0;new AlertDialog.Builder(this).setTitle("Sort songs by").setSingleChoiceItems(sorts,checked,(d,which)->{String value=new String[]{"TITLE","ARTIST","ALBUM","DURATION"}[which];getSharedPreferences("beatbox_settings",MODE_PRIVATE).edit().putString("song_sort",value).apply();app.io.execute(()->app.database().settings().set("song_sort",value));d.dismiss();}).setNegativeButton("Close",null).show();}
}
