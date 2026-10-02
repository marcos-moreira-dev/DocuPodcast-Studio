package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Lease supplied by application orchestration when an engine owns a scarce CPU/GPU resource. */
public interface ResourceLease extends AutoCloseable {
    ResourceLease NONE = () -> { };

    /** True when a conflicting higher-priority job should run at the next safe boundary. */
    default boolean yieldRequested() { return false; }

    /** Cooperative cancellation requested by the queue; observe only at a safe boundary. */
    default boolean cancellationRequested() { return false; }

    default String admissionId() { return ""; }

    /** Runtime confirms that the model represented by this lease is physically loaded. */
    default void confirmModelResident() { }

    /** Runtime confirms an actual unload; accounting is released at the safe lease boundary. */
    default void confirmModelUnloaded() { }

    @Override void close();
}
