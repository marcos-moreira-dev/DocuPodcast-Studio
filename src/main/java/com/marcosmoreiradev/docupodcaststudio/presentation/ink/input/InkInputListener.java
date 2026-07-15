package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import java.util.List;

public interface InkInputListener {
    default void onHover(InkInputSample sample) {
    }

    boolean onStrokeStart(InkInputSample sample);

    boolean onStrokeMove(InkInputSample sample);

    default boolean onStrokeMoveBatch(List<InkInputSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return false;
        }
        boolean consumed = false;
        for (InkInputSample sample : samples) {
            consumed |= onStrokeMove(sample);
        }
        return consumed;
    }

    boolean onStrokeEnd(InkInputSample sample);
}
