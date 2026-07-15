package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** How a theatre image model package is normally installed. */
public enum ImageModelPackageInstallType {
    AUTOMATIC_DOWNLOAD("Descarga automatica"),
    GATED_DOWNLOAD("Descarga con acceso"),
    MANUAL_IMPORT("Importacion manual"),
    CUSTOM_WORKFLOW("Workflow personalizado");

    private final String displayName;

    ImageModelPackageInstallType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
