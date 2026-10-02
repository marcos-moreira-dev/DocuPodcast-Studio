package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Back-to-front object order; selected objects retain their relative order. */
public final class CanvasObjectOrder {
    private CanvasObjectOrder() { }

    public static <T> List<T> reorder(List<T> source, Set<T> selected, int direction) {
        var result = new ArrayList<>(source);
        if (Math.abs(direction) == 2) {
            result.removeIf(selected::contains);
            var picked = source.stream().filter(selected::contains).toList();
            result.addAll(direction > 0 ? result.size() : 0, picked);
        } else if (direction == 1) {
            for (int i = result.size() - 2; i >= 0; i--) {
                if (selected.contains(result.get(i)) && !selected.contains(result.get(i + 1))) Collections.swap(result, i, i + 1);
            }
        } else if (direction == -1) {
            for (int i = 1; i < result.size(); i++) {
                if (selected.contains(result.get(i)) && !selected.contains(result.get(i - 1))) Collections.swap(result, i, i - 1);
            }
        }
        return result;
    }
}
