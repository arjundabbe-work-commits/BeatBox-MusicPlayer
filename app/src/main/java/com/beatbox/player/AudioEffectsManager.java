package com.beatbox.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import com.beatbox.player.domain.EqualizerSettings;

/** Owns audio effects for the active ExoPlayer audio session and service lifetime. */
@UnstableApi
final class AudioEffectsManager implements Player.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
    private final ExoPlayer player;
    private final SharedPreferences preferences;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private int sessionId = -1;

    AudioEffectsManager(Context context, ExoPlayer player) {
        this.player = player;
        preferences = context.getSharedPreferences("beatbox_settings", Context.MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        player.addListener(this);
        attach(player.getAudioSessionId());
    }

    @Override public void onAudioSessionIdChanged(int audioSessionId) { attach(audioSessionId); }
    @Override public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) attach(player.getAudioSessionId());
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.startsWith("audio_effect_")) apply();
    }

    private void attach(int newSessionId) {
        if (newSessionId <= 0) {
            if (sessionId > 0) releaseEffects();
            sessionId = -1;
            return;
        }
        if (newSessionId == sessionId) return;
        releaseEffects();
        sessionId = newSessionId;
        try { equalizer = new Equalizer(0, sessionId); } catch (RuntimeException ignored) { equalizer = null; }
        try { bassBoost = new BassBoost(0, sessionId); } catch (RuntimeException ignored) { bassBoost = null; }
        try { virtualizer = new Virtualizer(0, sessionId); } catch (RuntimeException ignored) { virtualizer = null; }
        apply();
    }

    private void apply() {
        if (sessionId <= 0) return;
        if (equalizer != null) try {
            int count = equalizer.getNumberOfBands();
            short[] range = equalizer.getBandLevelRange();
            int[] gains = new int[EqualizerSettings.BAND_COUNT];
            for (int index = 0; index < gains.length; index++) gains[index] = preferences.getInt("audio_effect_band_" + index, 0);
            if (equalizer.hasControl()) {
                for (short band = 0; band < count; band++) {
                    int normalized = EqualizerSettings.gainForDeviceBand(gains, band, count);
                    equalizer.setBandLevel(band, EqualizerSettings.toMillibels(normalized, range));
                }
                equalizer.setEnabled(preferences.getBoolean("audio_effect_equalizer_enabled", false));
            }
        } catch (RuntimeException ignored) { }
        if (bassBoost != null) try {
            if (bassBoost.hasControl()) {
                bassBoost.setStrength((short) Math.max(0, Math.min(1000, preferences.getInt("audio_effect_bass_strength", 0))));
                bassBoost.setEnabled(preferences.getBoolean("audio_effect_bass_enabled", false));
            }
        } catch (RuntimeException ignored) { }
        if (virtualizer != null) try {
            if (virtualizer.hasControl()) {
                virtualizer.setStrength((short) Math.max(0, Math.min(1000, preferences.getInt("audio_effect_space_strength", 0))));
                virtualizer.setEnabled(preferences.getBoolean("audio_effect_space_enabled", false));
            }
        } catch (RuntimeException ignored) { }
    }

    void release() {
        player.removeListener(this);
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        releaseEffects();
        sessionId = -1;
    }

    private void releaseEffects() {
        if (equalizer != null) { try { equalizer.release(); } catch (RuntimeException ignored) { } equalizer = null; }
        if (bassBoost != null) { try { bassBoost.release(); } catch (RuntimeException ignored) { } bassBoost = null; }
        if (virtualizer != null) { try { virtualizer.release(); } catch (RuntimeException ignored) { } virtualizer = null; }
    }
}
