package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import javafx.scene.Node;

import java.util.Optional;

/**
 * Reserved compatibility boundary for tablets that do not publish Windows Ink
 * packets. It stays opt-in and honest until a real Wintab bridge is connected.
 */
public final class WintabInkInputProvider implements InkInputProvider {
    private static final String ENABLE_PROPERTY = "docupodcast.ink.enableExperimentalWintab";
    private final JavaFxMouseInputProvider fallback = new JavaFxMouseInputProvider();

    private WintabInkInputProvider() {
    }

    public static Optional<InkInputProvider> tryCreate() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            return Optional.empty();
        }
        return Optional.of(new WintabInkInputProvider());
    }

    @Override
    public InkInputCapabilities capabilities() {
        return InkInputCapabilities.javafxMouse(
                "Wintab esta reservado como compatibilidad v2; esta compilacion no tiene listener Wintab conectado.");
    }

    @Override
    public void attach(Node target, InkInputListener listener) {
        fallback.attach(target, listener);
    }

    @Override
    public void detach() {
        fallback.detach();
    }
}
