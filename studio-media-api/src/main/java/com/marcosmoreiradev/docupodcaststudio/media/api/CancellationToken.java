package com.marcosmoreiradev.docupodcaststudio.media.api;

@FunctionalInterface
public interface CancellationToken {
    CancellationToken NONE = () -> false;

    boolean cancellationRequested();

    default void throwIfCancellationRequested() throws InterruptedException {
        if (cancellationRequested()) throw new InterruptedException("operation cancelled");
    }
}
