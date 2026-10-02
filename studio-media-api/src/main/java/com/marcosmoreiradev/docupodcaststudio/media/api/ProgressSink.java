package com.marcosmoreiradev.docupodcaststudio.media.api;

@FunctionalInterface
public interface ProgressSink {
    ProgressSink NONE = (stage, progress, message) -> { };

    void report(String stage, double progress, String message);
}
