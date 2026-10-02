package com.marcosmoreiradev.docupodcaststudio.launcher;

import javafx.application.Application;

/** Classpath-safe app-image entrypoint; the JavaFX application remains {@link StudioLauncher}. */
public final class StudioMain {
    private StudioMain() { }

    public static void main(String[] args) {
        Application.launch(StudioLauncher.class, args);
    }
}
