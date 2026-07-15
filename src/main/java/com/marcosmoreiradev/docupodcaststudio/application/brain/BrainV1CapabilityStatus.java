package com.marcosmoreiradev.docupodcaststudio.application.brain;

/** Product status of a frozen V1 capability. */
public enum BrainV1CapabilityStatus {
    V1_READY,
    V1_READY_WITH_LIMITS,
    V1_INTERNAL_ADVANCED,
    V2_DEFERRED;

    public boolean isAvailableInV1() {
        return this != V2_DEFERRED;
    }
}
