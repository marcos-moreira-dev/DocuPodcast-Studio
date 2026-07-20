package com.marcosmoreiradev.docupodcaststudio.ink.input;

import javafx.scene.Node;

final class UnavailableInkInputProvider implements InkInputProvider {
    private final InkInputCapabilities capabilities;

    UnavailableInkInputProvider(InkInputCapabilities capabilities) {
        this.capabilities = capabilities == null
                ? InkInputCapabilities.lectureStudioUnavailable("Proveedor de tinta no disponible.")
                : capabilities;
    }

    @Override
    public InkInputCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public void attach(Node target, InkInputListener listener) {
        // No native provider was available to attach.
    }

    @Override
    public void detach() {
        // Nothing to detach.
    }
}
