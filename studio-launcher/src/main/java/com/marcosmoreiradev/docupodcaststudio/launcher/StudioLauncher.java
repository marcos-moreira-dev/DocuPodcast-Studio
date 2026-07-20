package com.marcosmoreiradev.docupodcaststudio.launcher;

import com.marcosmoreiradev.docupodcaststudio.DocuPodcastStudioApp;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;

import java.nio.file.Path;

/** Production composition entry point for the built-in local media adapters. */
public final class StudioLauncher {
    private StudioLauncher() { }

    public static void main(String[] args) {
        Path applicationRoot = Path.of(".").toAbsolutePath().normalize();
        DocuPodcastStudioApp.configureMediaEngines(LocalMediaAdapters.create(applicationRoot));
        DocuPodcastStudioApp.main(args);
    }
}
