package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.scene.Node;

public final class NoopInkInputProvider implements InkInputProvider {
    public static final NoopInkInputProvider INSTANCE = new NoopInkInputProvider();

    private NoopInkInputProvider() {
    }

    @Override
    public InkInputCapabilities capabilities() {
        return InkInputCapabilities.javafxMouse("Proveedor nativo no disponible; usando JavaFX mouse directo.");
    }

    @Override
    public void attach(Node target, InkInputListener listener) {
        // Intentionally empty.
    }

    @Override
    public void detach() {
        // Intentionally empty.
    }
}
