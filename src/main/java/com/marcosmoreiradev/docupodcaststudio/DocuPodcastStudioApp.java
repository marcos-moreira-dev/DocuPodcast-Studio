package com.marcosmoreiradev.docupodcaststudio;

import com.marcosmoreiradev.docupodcaststudio.bootstrap.DesktopApplicationHost;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.ink.StudioInkPlatform;
import javafx.application.Application;
import javafx.stage.Stage;

/** Diagnostic desktop entry point. Production starts in {@code studio-launcher}. */
public final class DocuPodcastStudioApp extends Application {
    @Override public void start(Stage stage) {
        new DesktopApplicationHost(MediaEnginePlatform.empty(), StudioInkPlatform.local()).start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
