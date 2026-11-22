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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PlaybackServiceTest {
    private ListenableFuture<MediaController> controllerFuture;

    @Test public void controllerConnectsToTheSinglePlaybackServiceSession() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionToken token = new SessionToken(context, new ComponentName(context, PlaybackService.class));
        controllerFuture = new MediaController.Builder(context, token).buildAsync();
        MediaController controller = controllerFuture.get(10, TimeUnit.SECONDS);
        assertNotNull(controller);
        assertEquals(0, controller.getMediaItemCount());
        assertTrue(controller.isCommandAvailable(Player.COMMAND_PLAY_PAUSE));
        assertTrue(controller.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH));
    }

    @After public void releaseController() {
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
    }
}
