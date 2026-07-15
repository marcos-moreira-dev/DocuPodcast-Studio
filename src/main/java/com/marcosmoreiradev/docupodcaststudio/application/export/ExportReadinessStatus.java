package com.marcosmoreiradev.docupodcaststudio.application.export;

/** Status for one exportable artifact or for the aggregate export brain report. */
public enum ExportReadinessStatus {
    EXPORTABLE("Exportable"),
    EXPORTABLE_CON_ADVERTENCIAS("Exportable con advertencias"),
    BLOQUEADO("Bloqueado");

    private final String displayName;

    ExportReadinessStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean exportable() {
        return this == EXPORTABLE || this == EXPORTABLE_CON_ADVERTENCIAS;
    }

    public boolean blocked() {
        return this == BLOQUEADO;
    }
}
