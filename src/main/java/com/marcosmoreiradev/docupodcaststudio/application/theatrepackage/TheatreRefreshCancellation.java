package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

@FunctionalInterface
public interface TheatreRefreshCancellation {
    TheatreRefreshCancellation NEVER = () -> false;
    boolean cancelled();

    default void checkpoint() throws java.io.InterruptedIOException {
        if (cancelled()) throw new java.io.InterruptedIOException("Refresco teatral cancelado.");
    }
}
