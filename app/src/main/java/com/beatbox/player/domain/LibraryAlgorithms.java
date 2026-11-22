package com.beatbox.player.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class LibraryAlgorithms {
    public interface Labeler<T> { String label(T item); }
    private LibraryAlgorithms() { }

    public static <T> Map<String, List<T>> groupBy(List<T> items, Labeler<T> labeler) {
        TreeMap<String, List<T>> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (T item : items) {
            String label = labeler.label(item);
            if (label == null || label.trim().isEmpty()) label = "Unknown";
            List<T> group = sorted.get(label);
            if (group == null) {
                group = new ArrayList<>();
                sorted.put(label, group);
            }
            group.add(item);
        }
        return new LinkedHashMap<>(sorted);
    }
}
