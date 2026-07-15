package com.marcosmoreiradev.docupodcaststudio.application.engines;

/** Human-facing readiness state for voice/media engines. */
public enum HumanEnginePreflightState {
    READY("Listo"),
    REQUIRES_PREPARATION("Requiere preparación"),
    ERROR("Error");

    private final String label;

    HumanEnginePreflightState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
