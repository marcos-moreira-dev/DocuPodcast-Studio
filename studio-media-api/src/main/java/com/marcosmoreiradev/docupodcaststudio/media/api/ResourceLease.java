package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Lease supplied by application orchestration when an engine owns a scarce CPU/GPU resource. */
public interface ResourceLease extends AutoCloseable {
    ResourceLease NONE = () -> { };

    @Override void close();
}
