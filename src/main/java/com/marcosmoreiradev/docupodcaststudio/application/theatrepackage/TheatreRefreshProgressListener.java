package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

@FunctionalInterface
public interface TheatreRefreshProgressListener {
    TheatreRefreshProgressListener NONE = ignored -> { };
    void onProgress(TheatreRefreshProgress progress);
}
