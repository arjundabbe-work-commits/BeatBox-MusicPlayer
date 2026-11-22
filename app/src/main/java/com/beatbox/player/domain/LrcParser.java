package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {
    private static final Pattern TIMESTAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern METADATA = Pattern.compile("^\\[[a-zA-Z]{1,12}:[^\\]]*\\]$");

    public static final class Line {
        public final long timeMs;
        public final String text;
        public Line(long timeMs, String text) { this.timeMs = timeMs; this.text = text; }
    }

    private LrcParser() { }

    public static List<Line> parse(String content) {
        List<Line> lines = new ArrayList<>();
        if (content == null || content.isEmpty()) return lines;
        for (String raw : content.replace("\r", "").split("\n")) {
            String source = raw.trim();
            if (source.isEmpty()) continue;
            Matcher matcher = TIMESTAMP.matcher(source);
            List<Long> timestamps = new ArrayList<>();
            int textStart = 0;
            while (matcher.find()) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                String fraction = matcher.group(3);
                long fractionMs = fraction == null ? 0 : fraction.length() == 1
                        ? Long.parseLong(fraction) * 100 : fraction.length() == 2
                        ? Long.parseLong(fraction) * 10 : Long.parseLong(fraction);
                timestamps.add(minutes * 60_000 + seconds * 1000 + fractionMs);
                textStart = matcher.end();
            }
            String text = source.substring(textStart).trim();
            if (!timestamps.isEmpty()) {
                for (Long time : timestamps) lines.add(new Line(time, text));
            } else if (!METADATA.matcher(source).matches()) {
                lines.add(new Line(-1, source));
            }
        }
        lines.sort((left, right) -> Long.compare(left.timeMs < 0 ? Long.MAX_VALUE : left.timeMs,
                right.timeMs < 0 ? Long.MAX_VALUE : right.timeMs));
        return lines;
    }
}
