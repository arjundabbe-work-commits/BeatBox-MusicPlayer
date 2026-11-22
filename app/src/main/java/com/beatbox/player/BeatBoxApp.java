package com.beatbox.player;

import android.app.Application;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.room.Room;
import com.beatbox.player.data.BeatBoxDatabase;
import com.beatbox.player.domain.SleepTimer;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public final class BeatBoxApp extends Application {
    public interface LibraryListener { void onLibraryChanged(List<Song> songs); }
    public final ExecutorService io = Executors.newFixedThreadPool(2);
    private final CopyOnWriteArrayList<LibraryListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile List<Song> songs = Collections.emptyList();
    private BeatBoxDatabase database;
    private static final String TIMER_PREFS = "beatbox_sleep_timer";
    private static final String TIMER_DEADLINE = "deadline_epoch_ms";
    private final Runnable sleepTimerAction = this::pausePlaybackForSleepTimer;

    @Override public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(), BeatBoxDatabase.class, "beatbox.db").build();
        resumeSleepTimer();
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

    public void startSleepTimer(long durationMs) {
        long deadline = SleepTimer.deadlineAfter(System.currentTimeMillis(), durationMs);
        timerPreferences().edit().putLong(TIMER_DEADLINE, deadline).apply();
        mainHandler.removeCallbacks(sleepTimerAction);
        mainHandler.postDelayed(sleepTimerAction, SleepTimer.remaining(deadline, System.currentTimeMillis()));
    }

    public void cancelSleepTimer() {
        mainHandler.removeCallbacks(sleepTimerAction);
        timerPreferences().edit().remove(TIMER_DEADLINE).apply();
    }

    public long sleepTimerRemainingMs() {
        long deadline = timerPreferences().getLong(TIMER_DEADLINE, 0);
        long remaining = SleepTimer.remaining(deadline, System.currentTimeMillis());
        if (remaining == 0 && deadline != 0) timerPreferences().edit().remove(TIMER_DEADLINE).apply();
        return remaining;
    }

    private SharedPreferences timerPreferences() { return getSharedPreferences(TIMER_PREFS, MODE_PRIVATE); }

    private void resumeSleepTimer() {
        long deadline = timerPreferences().getLong(TIMER_DEADLINE, 0);
        if (deadline == 0) return;
        long remaining = SleepTimer.remaining(deadline, System.currentTimeMillis());
        if (remaining == 0) {
            timerPreferences().edit().remove(TIMER_DEADLINE).apply();
        } else {
            mainHandler.postDelayed(sleepTimerAction, remaining);
        }
    }

    private void pausePlaybackForSleepTimer() {
        timerPreferences().edit().remove(TIMER_DEADLINE).apply();
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        ListenableFuture<MediaController> future = new MediaController.Builder(this, token).buildAsync();
        future.addListener(() -> mainHandler.post(() -> {
            try { future.get().pause(); } catch (Exception ignored) { }
            MediaController.releaseFuture(future);
        }), command -> command.run());
    }
}
