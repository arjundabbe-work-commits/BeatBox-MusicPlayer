package com.beatbox.player.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class SleepTimerTest {
    @Test public void deadlineAndRemainingUseTheProvidedClock() {
        long deadline = SleepTimer.deadlineAfter(10_000, 60_000);
        assertEquals(70_000, deadline);
        assertEquals(35_000, SleepTimer.remaining(deadline, 35_000));
        assertEquals(0, SleepTimer.remaining(deadline, 70_000));
        assertEquals(0, SleepTimer.remaining(deadline, 80_000));
    }

    @Test public void rejectsZeroAndNegativeDurations() {
        for (long duration : new long[]{0, -1}) {
            try { SleepTimer.deadlineAfter(10, duration); fail("duration must be positive"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void saturatesDeadlineOnOverflow() {
        assertEquals(Long.MAX_VALUE, SleepTimer.deadlineAfter(Long.MAX_VALUE - 10, 20));
    }
}
