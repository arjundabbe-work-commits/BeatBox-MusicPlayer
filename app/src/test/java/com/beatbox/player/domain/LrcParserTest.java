package com.beatbox.player.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class LrcParserTest {
    @Test public void parsesAndOrdersTimestampedLines() {
        List<LrcParser.Line> lines = LrcParser.parse("[ar:Performer]\n[00:02.50]Second\n[00:01.2][00:03.00]First");
        assertEquals(3, lines.size());
        assertEquals(1200, lines.get(0).timeMs);
        assertEquals("First", lines.get(0).text);
        assertEquals(2500, lines.get(1).timeMs);
        assertEquals(3000, lines.get(2).timeMs);
    }

    @Test public void leavesUnsynchronisedTextUntimedAndHandlesEmptyInput() {
        List<LrcParser.Line> lines = LrcParser.parse("A lyric line\n\nAnother line");
        assertEquals(2, lines.size());
        assertEquals(-1, lines.get(0).timeMs);
        assertEquals("Another line", lines.get(1).text);
        assertTrue(LrcParser.parse(null).isEmpty());
    }
}
