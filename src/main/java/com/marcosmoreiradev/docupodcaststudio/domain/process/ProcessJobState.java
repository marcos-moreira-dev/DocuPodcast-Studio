package com.marcosmoreiradev.docupodcaststudio.domain.process;

/** Generic lifecycle for local process jobs, regardless of the concrete engine. */
public enum ProcessJobState {
    IDLE("Sin trabajo", false, false),
    QUEUED("En cola", true, false),
    PREPARING("Preparando", true, false),
    RUNNING("Ejecutando", true, false),
    CANCELLATION_REQUESTED("Cancelación solicitada", true, false),
    CANCELLED("Cancelado", false, true),
    COMPLETED("Completado", false, true),
    FAILED("Fallido", false, true),
    NEEDS_USER_ACTION("Requiere accion del usuario", false, false),
    SKIPPED("Omitido", false, true);

    private final String displayName;
    private final boolean running;
    private final boolean terminal;

    ProcessJobState(String displayName, boolean running, boolean terminal) {
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
