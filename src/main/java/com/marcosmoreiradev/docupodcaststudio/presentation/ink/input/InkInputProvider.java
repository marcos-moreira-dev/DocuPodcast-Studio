package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import javafx.scene.Node;

public interface InkInputProvider extends AutoCloseable {
    InkInputCapabilities capabilities();

    void attach(Node target, InkInputListener listener);

    void detach();

    default void resetCoordinateState() {
        // Providers without native coordinate state do not need to react.
    }

    @Override
    default void close() {
        detach();
    }
}
