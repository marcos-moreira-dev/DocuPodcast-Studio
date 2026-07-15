package com.marcosmoreiradev.docupodcaststudio.application.project;

/** Severity for a project integrity issue. */
public enum ProjectIntegritySeverity {
    INFO("Informativo", false),
    WARNING("Advertencia", false),
    ERROR("Requiere reparación", true);

    private final String displayName;
    private final boolean blocking;

    ProjectIntegritySeverity(String displayName, boolean blocking) {
        this.displayName = displayName;
        this.blocking = blocking;
    }

    public String displayName() {
        return displayName;
    }

    public boolean blocking() {
        return blocking;
    }
}
