package com.beatbox.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public final class SettingsActivity extends AppCompatActivity {
    private BeatBoxApp app;
    private TextView timerStatus;
    private final String[] sorts = {"Title", "Artist", "Album", "Duration"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimerStatus = new Runnable() {
        @Override public void run() {
            if (timerStatus != null) {
                long remaining = app.sleepTimerRemainingMs();
                timerStatus.setText(remaining == 0 ? "No sleep timer" : "Pauses playback in " + formatRemaining(remaining));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        app = (BeatBoxApp) getApplication();
        timerStatus = findViewById(R.id.sleep_timer_status);
        findViewById(R.id.sort_order).setOnClickListener(v -> chooseSort());
        findViewById(R.id.sleep_timer).setOnClickListener(v -> chooseSleepTimer());
        findViewById(R.id.clear_history).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("Clear recently played history?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (d, w) -> app.io.execute(() -> app.database().history().clear()))
                .show());
        findViewById(R.id.open_about).setOnClickListener(v -> startActivity(new android.content.Intent(this, AboutUsActivity.class)));
    }

    private void chooseSort() {
        String selected = getSharedPreferences("beatbox_settings", MODE_PRIVATE).getString("song_sort", "TITLE");
        int checked = selected.equals("ARTIST") ? 1 : selected.equals("ALBUM") ? 2 : selected.equals("DURATION") ? 3 : 0;
        new AlertDialog.Builder(this).setTitle("Sort songs by").setSingleChoiceItems(sorts, checked, (d, which) -> {
            String value = new String[]{"TITLE", "ARTIST", "ALBUM", "DURATION"}[which];
            getSharedPreferences("beatbox_settings", MODE_PRIVATE).edit().putString("song_sort", value).apply();
            app.io.execute(() -> app.database().settings().set("song_sort", value));
            d.dismiss();
        }).setNegativeButton("Close", null).show();
    }

    private void chooseSleepTimer() {
        String[] choices = {"15 minutes", "30 minutes", "45 minutes", "60 minutes", "Custom…", "Cancel timer"};
        new AlertDialog.Builder(this).setTitle("Pause playback after")
                .setItems(choices, (dialog, which) -> {
                    if (which < 4) startTimer((which + 1L) * 15L);
                    else if (which == 4) showCustomTimer();
                    else { app.cancelSleepTimer(); refreshTimerStatus(); }
                }).show();
    }

    private void showCustomTimer() {
        EditText minutes = new EditText(this);
        minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutes.setHint("Minutes (1–720)");
        minutes.setSingleLine(true);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        minutes.setPadding(padding, padding, padding, padding);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Custom sleep timer")
                .setView(minutes).setNegativeButton("Cancel", null)
                .setPositiveButton("Start", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = minutes.getText().toString().trim();
            int requested;
            try { requested = Integer.parseInt(value); }
            catch (NumberFormatException error) { minutes.setError("Enter a number of minutes"); return; }
            if (requested < 1 || requested > 720) { minutes.setError("Choose 1 to 720 minutes"); return; }
            startTimer(requested);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void startTimer(long minutes) {
        app.startSleepTimer(minutes * 60_000L);
        Toast.makeText(this, "Playback will pause in " + minutes + " minutes.", Toast.LENGTH_SHORT).show();
        refreshTimerStatus();
    }

    private void refreshTimerStatus() {
        handler.removeCallbacks(updateTimerStatus);
        updateTimerStatus.run();
    }

    private static String formatRemaining(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override protected void onResume() { super.onResume(); refreshTimerStatus(); }
    @Override protected void onPause() { handler.removeCallbacks(updateTimerStatus); super.onPause(); }
}
