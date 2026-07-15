package com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input;

public final class InkInputProviderFactory {
    private InkInputProviderFactory() {
    }

    public static InkInputProvider createDefault() {
        return new JavaFxMouseInputProvider();
    }
}
