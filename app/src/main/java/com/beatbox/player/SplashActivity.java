package com.beatbox.player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
public final class SplashActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) { startActivity(new Intent(this, HomeActivity.class)); finish(); }
        }, 900);
    }
}
