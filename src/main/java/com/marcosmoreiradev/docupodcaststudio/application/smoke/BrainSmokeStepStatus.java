package com.marcosmoreiradev.docupodcaststudio.application.smoke;

/** Result category for one automatic brain-smoke step. */
public enum BrainSmokeStepStatus {
    PASSED("OK"),
    WARNING("Advertencia"),
    FAILED("Fallo");

    private final String displayName;

    BrainSmokeStepStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean successful() {
        return this != FAILED;
    }
}
