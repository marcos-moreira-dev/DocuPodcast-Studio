package com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input;

public record InkInputCapabilities(
        String providerName,
        boolean pressure,
        boolean tilt,
        boolean eraserCursor,
        boolean nativeProvider) {

    public static InkInputCapabilities javafxMouse() {
        return new InkInputCapabilities("JavaFX mouse", false, false, false, false);
    }
}
