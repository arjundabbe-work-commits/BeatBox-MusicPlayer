package com.beatbox.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PlaybackServiceTest {
    private ListenableFuture<MediaController> controllerFuture;

    @Test public void controllerConnectsToTheSinglePlaybackServiceSession() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionToken token = new SessionToken(context, new ComponentName(context, PlaybackService.class));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                controllerFuture = new MediaController.Builder(context, token).buildAsync());
        MediaController controller = controllerFuture.get(10, TimeUnit.SECONDS);
        assertNotNull(controller);
        AtomicInteger itemCount = new AtomicInteger(-1);
        AtomicBoolean playPauseAvailable = new AtomicBoolean();
        AtomicBoolean speedAvailable = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            itemCount.set(controller.getMediaItemCount());
            playPauseAvailable.set(controller.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
            speedAvailable.set(controller.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH));
        });
        assertEquals(0, itemCount.get());
        assertTrue(playPauseAvailable.get());
        assertTrue(speedAvailable.get());
    }

    @After public void releaseController() {
        if (controllerFuture != null) InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> MediaController.releaseFuture(controllerFuture));
    }
}
