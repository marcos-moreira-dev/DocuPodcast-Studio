package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Acquires scarce resources inside worker threads, never from presentation. */
public interface ResourceScheduler {
    ResourceLease acquire(ResourceRequirement requirement, CancellationToken cancellation)
            throws InterruptedException;
}
