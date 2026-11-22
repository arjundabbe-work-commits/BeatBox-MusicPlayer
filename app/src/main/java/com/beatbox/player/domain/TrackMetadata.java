package com.beatbox.player.domain;

public interface TrackMetadata {
    String title();
    String artist();
    String album();
    long durationMs();
    long stableId();
}
