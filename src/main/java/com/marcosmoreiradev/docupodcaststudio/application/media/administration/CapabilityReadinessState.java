package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

/** User-facing state of one concrete local capability requirement. */
public enum CapabilityReadinessState {
    READY,
    DEGRADED,
    MISSING,
    INVALID,
    STOPPED,
    RUNNING,
    CONFLICT
}
