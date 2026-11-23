package com.beatbox.player;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.widget.ListView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class BeatBoxUiTest {
    @Before public void grantAudioAccessForUiFlows() throws Exception {
        android.app.UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try (android.os.ParcelFileDescriptor ignored = automation.executeShellCommand("input keyevent KEYCODE_WAKEUP")) { }
        try (android.os.ParcelFileDescriptor ignored = automation.executeShellCommand("wm dismiss-keyguard")) { }
        if (Build.VERSION.SDK_INT < 26) return;
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String permission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        automation.grantRuntimePermission(context.getPackageName(), permission);
    }

    @Test public void libraryCanOpenAlbumCategoryAndReturnToCategories() {
        try (ActivityScenario<LibraryActivity> scenario = ActivityScenario.launch(LibraryActivity.class)) {
            onData(anything()).inAdapterView(withId(R.id.library_items)).atPosition(1).perform(click());
            onView(withId(R.id.library_heading)).check(matches(withText("Albums")));
            onView(withId(R.id.library_message)).check(matches(withText(containsString("albums"))));
            onView(withId(R.id.library_back)).perform(click());
            onView(withId(R.id.library_heading)).check(matches(withText("Your Library")));
        }
    }

    @Test public void customSleepTimerShowsRemainingTimeAndCanBeCancelled() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.sleep_timer)).perform(click());
            onView(withText("Custom…")).perform(click());
            onView(withHint("Minutes (1–720)")).perform(typeText("5"), closeSoftKeyboard());
            onView(withText("Start")).perform(click());
            onView(withId(R.id.sleep_timer_status)).check(matches(withText(startsWith("Pauses playback in"))));
            onView(withId(R.id.sleep_timer)).perform(click());
            onView(withText("Cancel timer")).perform(click());
            onView(withId(R.id.sleep_timer_status)).check(matches(withText("No sleep timer")));
        }
    }

    @Test public void searchFiltersOutAnUnmatchedLocalQuery() {
        try (ActivityScenario<SearchActivity> scenario = ActivityScenario.launch(SearchActivity.class)) {
            onView(withId(R.id.search_query)).perform(typeText("BeatBox query with no matching local title"), closeSoftKeyboard());
            SystemClock.sleep(300);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertEquals(0, ((ListView) activity.findViewById(R.id.search_results)).getCount()));
        }
    }

    @Test public void lyricsScreenExplainsWhenNoTrackIsPlaying() {
        try (ActivityScenario<LyricsActivity> scenario = ActivityScenario.launch(LyricsActivity.class)) {
            SystemClock.sleep(300);
            onView(withText(startsWith("No song is playing"))).check(matches(isDisplayed()));
        }
    }

    @Test public void playlistTitleAppearsBelowStatusBarInsets() {
        try (ActivityScenario<PlaylistsActivity> scenario = ActivityScenario.launch(PlaylistsActivity.class)) {
            scenario.onActivity(activity -> {
                TextView title = activity.findViewById(R.id.playlists_title);
                int[] location = new int[2];
                title.getLocationOnScreen(location);
                android.view.WindowInsets insets = activity.getWindow().getDecorView().getRootWindowInsets();
                int statusBarBottom = insets == null ? 0 : insets.getSystemWindowInsetTop();
                org.junit.Assert.assertTrue("Playlist title overlaps the status bar", location[1] >= statusBarBottom);
            });
        }
    }
}
