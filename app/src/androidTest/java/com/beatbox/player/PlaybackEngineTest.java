package com.beatbox.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import android.os.ConditionVariable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PlaybackEngineTest {
    private ExoPlayer player;
    private File audioFile;

    @Before public void createPlayer() throws Exception {
        Context testContext=InstrumentationRegistry.getInstrumentation().getContext();
        Context targetContext=InstrumentationRegistry.getInstrumentation().getTargetContext();
        // ExoPlayer reads the fixture as the target application, so place it in
        // that package's cache rather than the separate instrumentation APK cache.
        audioFile=new File(targetContext.getCacheDir(),"test-tone.wav");
        try(InputStream input=testContext.getAssets().open("test-tone.wav");FileOutputStream output=new FileOutputStream(audioFile)){
            byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
        }
        player=new ExoPlayer.Builder(targetContext).build();
    }
    @After public void releasePlayer(){if(player!=null)player.release();if(audioFile!=null)audioFile.delete();}

    @Test public void decodesLocalAudioAndSupportsSeekPauseShuffleAndRepeat() throws Exception {
        ConditionVariable ready=new ConditionVariable();
        player.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_READY)ready.open();}});
        MediaItem item=MediaItem.fromUri(Uri.fromFile(audioFile));
        player.setMediaItems(Arrays.asList(item,item));player.prepare();
        assertTrue("local WAV should become ready",ready.block(5000));
        assertTrue("decoded duration should be positive",player.getDuration()>0);
        player.seekTo(600);
        player.setPlayWhenReady(true);
        long deadline=System.currentTimeMillis()+3000;
        while(!player.isPlaying()&&System.currentTimeMillis()<deadline)Thread.sleep(30);
        assertTrue("ExoPlayer should start the local test fixture",player.isPlaying());
        player.pause();assertTrue(!player.isPlaying());
        player.setShuffleModeEnabled(true);assertTrue(player.getShuffleModeEnabled());
        player.setRepeatMode(Player.REPEAT_MODE_ALL);assertEquals(Player.REPEAT_MODE_ALL,player.getRepeatMode());
        player.setRepeatMode(Player.REPEAT_MODE_ONE);assertEquals(Player.REPEAT_MODE_ONE,player.getRepeatMode());
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
    }
}
