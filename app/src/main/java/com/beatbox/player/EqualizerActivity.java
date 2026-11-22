package com.beatbox.player;

import android.content.ComponentName;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.beatbox.player.domain.EqualizerSettings;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@UnstableApi
public final class EqualizerActivity extends AppCompatActivity {
    private static final List<String> PRESETS = Arrays.asList("Flat", "Bass", "Vocal", "Rock", "Classical", "Custom");
    private static final String[] BAND_LABELS = {"Low", "Low-mid", "Mid", "High-mid", "High"};
    private BeatBoxApp app;
    private android.content.SharedPreferences preferences;
    private SwitchCompat equalizerSwitch, bassSwitch, spaceSwitch;
    private SeekBar bassStrength, spaceStrength;
    private Spinner presetSpinner;
    private TextView status;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean binding;
    private boolean started;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_equalizer);
        app = (BeatBoxApp) getApplication();
        preferences = getSharedPreferences("beatbox_settings", MODE_PRIVATE);
        status = findViewById(R.id.effect_status);
        equalizerSwitch = findViewById(R.id.equalizer_enabled);
        bassSwitch = findViewById(R.id.bass_enabled);
        spaceSwitch = findViewById(R.id.space_enabled);
        bassStrength = findViewById(R.id.bass_strength);
        spaceStrength = findViewById(R.id.space_strength);
        presetSpinner = findViewById(R.id.equalizer_preset);
        createBands(); configurePresetChoices(); bindStoredValues(); bindPresetSelection(); configureCapabilityState();
        equalizerSwitch.setOnCheckedChangeListener((button, checked) -> save("audio_effect_equalizer_enabled", checked));
        bassSwitch.setOnCheckedChangeListener((button, checked) -> save("audio_effect_bass_enabled", checked));
        spaceSwitch.setOnCheckedChangeListener((button, checked) -> save("audio_effect_space_enabled", checked));
        bindStrength(bassStrength, "audio_effect_bass_strength");
        bindStrength(spaceStrength, "audio_effect_space_strength");
        findViewById(R.id.equalizer_reset).setOnClickListener(view -> resetDefaults());
    }

    private void createBands() {
        LinearLayout container = findViewById(R.id.equalizer_bands);
        for (int index = 0; index < EqualizerSettings.BAND_COUNT; index++) {
            final int band = index;
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
            TextView label = new TextView(this); label.setTextColor(0xFFFFFFFF); label.setText(BAND_LABELS[index]);
            SeekBar slider = new SeekBar(this); slider.setMax(200); slider.setProgress(100);
            slider.setContentDescription(BAND_LABELS[index] + " equalizer level");
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int gain = progress - 100; label.setText(BAND_LABELS[band] + "  " + (gain > 0 ? "+" : "") + gain);
                    if (fromUser) {
                        preferences.edit().putInt("audio_effect_band_" + band, gain)
                                .putString("audio_effect_preset", "Custom").apply();
                        binding = true; presetSpinner.setSelection(PRESETS.indexOf("Custom")); binding = false;
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    save("audio_effect_band_" + band, Integer.toString(seekBar.getProgress() - 100));
                    save("audio_effect_preset", "Custom");
                }
            });
            row.addView(label); row.addView(slider);
            container.addView(row);
        }
    }

    private void configurePresetChoices() {
        presetSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PRESETS));
    }

    private void bindPresetSelection() {
        presetSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (binding) return;
                String name = PRESETS.get(position);
                preferences.edit().putString("audio_effect_preset", name).apply();
                if (!"Custom".equals(name)) {
                    int[] values = EqualizerSettings.preset(name);
                    android.content.SharedPreferences.Editor editor = preferences.edit();
                    for (int band = 0; band < values.length; band++) editor.putInt("audio_effect_band_" + band, values[band]);
                    editor.apply();
                    updateBandSliders(values);
                    saveRoom("audio_effect_preset", name);
                    for (int band = 0; band < values.length; band++) saveRoom("audio_effect_band_" + band, Integer.toString(values[band]));
                } else saveRoom("audio_effect_preset", name);
            }
        });
    }

    private void bindStoredValues() {
        binding = true;
        equalizerSwitch.setChecked(preferences.getBoolean("audio_effect_equalizer_enabled", false));
        bassSwitch.setChecked(preferences.getBoolean("audio_effect_bass_enabled", false));
        spaceSwitch.setChecked(preferences.getBoolean("audio_effect_space_enabled", false));
        bassStrength.setProgress(preferences.getInt("audio_effect_bass_strength", 0));
        spaceStrength.setProgress(preferences.getInt("audio_effect_space_strength", 0));
        int[] gains = new int[EqualizerSettings.BAND_COUNT];
        for (int band = 0; band < gains.length; band++) gains[band] = preferences.getInt("audio_effect_band_" + band, 0);
        updateBandSliders(gains);
        int preset = PRESETS.indexOf(preferences.getString("audio_effect_preset", "Flat"));
        presetSpinner.setSelection(Math.max(0, preset));
        binding = false;
    }

    private void updateBandSliders(int[] gains) {
        LinearLayout container = findViewById(R.id.equalizer_bands);
        for (int index = 0; index < Math.min(gains.length, container.getChildCount()); index++) {
            LinearLayout row = (LinearLayout) container.getChildAt(index);
            TextView label = (TextView) row.getChildAt(0);
            SeekBar slider = (SeekBar) row.getChildAt(1);
            int gain = EqualizerSettings.clampGain(gains[index]);
            slider.setProgress(gain + 100);
            label.setText(BAND_LABELS[index] + "  " + (gain > 0 ? "+" : "") + gain);
        }
    }

    private void bindStrength(SeekBar bar, String key) {
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) preferences.edit().putInt(key, value).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { save(key, Integer.toString(seekBar.getProgress())); }
        });
    }

    private void configureCapabilityState() {
        boolean hasEqualizer = supports(AudioEffect.EFFECT_TYPE_EQUALIZER);
        boolean hasBass = supports(AudioEffect.EFFECT_TYPE_BASS_BOOST);
        boolean hasVirtualizer = supports(AudioEffect.EFFECT_TYPE_VIRTUALIZER);
        equalizerSwitch.setEnabled(hasEqualizer); bassSwitch.setEnabled(hasBass); bassStrength.setEnabled(hasBass);
        spaceSwitch.setEnabled(hasVirtualizer); spaceStrength.setEnabled(hasVirtualizer);
        status.setText("Available effects: " + (hasEqualizer ? "equalizer" : "no equalizer") + " · "
                + (hasBass ? "bass boost" : "no bass boost") + " · "
                + (hasVirtualizer ? "virtualizer" : "no virtualizer")
                + "\nSettings are saved for the playback service's active audio session. Device support varies.");
    }

    private static boolean supports(UUID type) {
        try {
            AudioEffect.Descriptor[] descriptors = AudioEffect.queryEffects();
            if (descriptors != null) for (AudioEffect.Descriptor descriptor : descriptors) if (type.equals(descriptor.type)) return true;
        } catch (RuntimeException ignored) { }
        return false;
    }

    private void resetDefaults() {
        android.content.SharedPreferences.Editor editor = preferences.edit()
                .putBoolean("audio_effect_equalizer_enabled", false)
                .putBoolean("audio_effect_bass_enabled", false)
                .putBoolean("audio_effect_space_enabled", false)
                .putInt("audio_effect_bass_strength", 0)
                .putInt("audio_effect_space_strength", 0)
                .putString("audio_effect_preset", "Flat");
        for (int band = 0; band < EqualizerSettings.BAND_COUNT; band++) editor.putInt("audio_effect_band_" + band, 0);
        editor.apply();
        bindStoredValues();
        saveRoom("audio_effect_preset", "Flat");
        for (int band = 0; band < EqualizerSettings.BAND_COUNT; band++) saveRoom("audio_effect_band_" + band, "0");
        saveRoom("audio_effect_equalizer_enabled", "false");
        saveRoom("audio_effect_bass_enabled", "false"); saveRoom("audio_effect_space_enabled", "false");
        saveRoom("audio_effect_bass_strength", "0"); saveRoom("audio_effect_space_strength", "0");
    }

    private void save(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply(); saveRoom(key, Boolean.toString(value));
    }
    private void save(String key, String value) {
        if (key.startsWith("audio_effect_band_")) preferences.edit().putInt(key, Integer.parseInt(value)).apply();
        else if (key.endsWith("_strength")) preferences.edit().putInt(key, Integer.parseInt(value)).apply();
        else preferences.edit().putString(key, value).apply();
        saveRoom(key, value);
    }
    private void saveRoom(String key, String value) { app.io.execute(() -> app.database().settings().set(key, value)); }

    @Override protected void onStart() {
        super.onStart(); started = true;
        controllerFuture = new MediaController.Builder(this,
                new SessionToken(this, new ComponentName(this, PlaybackService.class))).buildAsync();
        controllerFuture.addListener(() -> {
            if (!started || isFinishing() || isDestroyed()) return;
            try {
                controller = controllerFuture.get();
                int session = controller.getAudioSessionId();
                if (session > 0) status.append("\nConnected to playback session " + session + ".");
                else status.append("\nStart a song to activate session-scoped effects.");
            } catch (Exception ignored) { status.append("\nPlayback service is not connected."); }
        }, command -> new android.os.Handler(getMainLooper()).post(command));
    }
    @Override protected void onStop() {
        started = false;
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); controllerFuture = null; }
        controller = null; super.onStop();
    }
}
