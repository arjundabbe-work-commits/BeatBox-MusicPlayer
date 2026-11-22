package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public final class LibraryAlgorithms {
    private LibraryAlgorithms() { }

    public static <T> Map<String, List<T>> groupBy(List<T> items, Function<T, String> labeler) {
        TreeMap<String, List<T>> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (T item : items) {
            String label = labeler.apply(item);
            if (label == null || label.trim().isEmpty()) label = "Unknown";
            sorted.computeIfAbsent(label, ignored -> new ArrayList<>()).add(item);
        }
        return new LinkedHashMap<>(sorted);
    }
}
