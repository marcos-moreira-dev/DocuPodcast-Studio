package com.marcosmoreiradev.docupodcaststudio.application.project;

/** Overall project integrity result. */
public enum ProjectIntegrityStatus {
    OK("OK"),
    CON_ADVERTENCIAS("Con advertencias"),
    REQUIERE_REPARACION("Requiere reparación");

    private final String displayName;

    ProjectIntegrityStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
