package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import java.time.Duration;

/** A narrow shutdown participant composed by the launcher. */
public interface LifecycleParticipant extends AutoCloseable {
    default void stopAcceptingWork() { }
    default void requestCancellation() { }
    default boolean awaitTermination(Duration remaining) throws InterruptedException { return true; }
    default void persistState() throws Exception { }
    @Override default void close() throws Exception { }
}
