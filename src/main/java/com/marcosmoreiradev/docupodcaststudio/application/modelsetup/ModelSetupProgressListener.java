package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** Receives human-readable progress updates from long local model/runtime setup operations. */
@FunctionalInterface
public interface ModelSetupProgressListener {
    void onProgress(String message);

    static ModelSetupProgressListener noop() {
        return message -> { };
    }
}
