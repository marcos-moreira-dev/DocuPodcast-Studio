package com.marcosmoreiradev.docupodcaststudio.domain.audio;

/** Lifecycle state for a local audio generation job. */
public enum AudioJobState {
    IDLE("Sin trabajo", false, false),
    QUEUED("En cola", true, false),
    PREPARING("Preparando", true, false),
    GENERATING_AUDIO("Generando audio", true, false),
    MERGING_AUDIO("Uniendo audio", true, false),
    CANCELLATION_REQUESTED("Cancelación solicitada", true, false),
    CANCELLED("Cancelado", false, true),
    COMPLETED("Completado", false, true),
    FAILED("Fallido", false, true);

    private final String displayName;
    private final boolean running;
    private final boolean terminal;

    AudioJobState(String displayName, boolean running, boolean terminal) {
        this.displayName = displayName;
        this.running = running;
        this.terminal = terminal;
    }

    public String displayName() {
        return displayName;
    }

    public boolean running() {
        return running;
    }

    public boolean terminal() {
        return terminal;
    }

    public boolean cancellable() {
        return running;
    }
}
