package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** High-level user-facing state for the local theatre image engine. */
public enum ImageEngineRuntimeState {
    NOT_PREPARED("No preparado"),
    RUNTIME_PREPARED("Runtime preparado"),
    MODEL_INSTALLED("Modelo instalado"),
    ENGINE_STARTED("Motor iniciado"),
    READY("Listo"),
    ERROR("Error");

    private final String displayName;

    ImageEngineRuntimeState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
