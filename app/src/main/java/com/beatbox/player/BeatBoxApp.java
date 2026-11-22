package com.beatbox.player;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.room.Room;
import com.beatbox.player.data.BeatBoxDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BeatBoxApp extends Application {
    public interface LibraryListener { void onLibraryChanged(List<Song> songs); }
    public final ExecutorService io = Executors.newFixedThreadPool(2);
    private final CopyOnWriteArrayList<LibraryListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile List<Song> songs = Collections.emptyList();
    private BeatBoxDatabase database;

    @Override public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(), BeatBoxDatabase.class, "beatbox.db").build();
    }
    public BeatBoxDatabase database() { return database; }
    public List<Song> songs() { return new ArrayList<>(songs); }
    public void updateLibrary(List<Song> updated) {
        songs = Collections.unmodifiableList(new ArrayList<>(updated));
        List<Song> snapshot = songs();
        mainHandler.post(() -> { for (LibraryListener listener : listeners) listener.onLibraryChanged(snapshot); });
    }
    public void addLibraryListener(LibraryListener listener) { listeners.add(listener); List<Song> current = songs(); mainHandler.post(() -> { if (listeners.contains(listener)) listener.onLibraryChanged(current); }); }
    public void removeLibraryListener(LibraryListener listener) { listeners.remove(listener); }
}
