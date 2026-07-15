package com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input;

import javafx.scene.Node;

public interface InkInputProvider extends AutoCloseable {
    InkInputCapabilities capabilities();

    void attach(Node target, InkInputListener listener);

    void detach();

    @Override
    default void close() {
        detach();
    }
}
