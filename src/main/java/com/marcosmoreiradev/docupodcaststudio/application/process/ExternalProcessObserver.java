package com.marcosmoreiradev.docupodcaststudio.application.process;

/** Observes a local external process without coupling callers to the JDK process API. */
public interface ExternalProcessObserver {
    ExternalProcessObserver NOOP = new ExternalProcessObserver() {
    };

    static ExternalProcessObserver noop() {
        return NOOP;
    }

    default void onOutputLine(String line) {
        // Optional progress hook.
    }

    default boolean cancellationRequested() {
        return false;
    }
}
