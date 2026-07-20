package com.marcosmoreiradev.docupodcaststudio.launcher;

import com.marcosmoreiradev.docupodcaststudio.bootstrap.DesktopApplicationHost;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.ink.StudioInkPlatform;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;

/** Single production composition root for desktop, media adapters and engine administration. */
public final class StudioLauncher extends Application {
    @Override public void start(Stage stage) {
        Path applicationRoot = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        StudioInkPlatform ink = StudioInkPlatform.local();
        new DesktopApplicationHost(LocalMediaAdapters.create(applicationRoot), ink).start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
