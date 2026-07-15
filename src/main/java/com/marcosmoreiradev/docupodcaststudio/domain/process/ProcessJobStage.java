package com.marcosmoreiradev.docupodcaststudio.domain.process;

/** Coarse, cross-engine stage for progress views and diagnostics. */
public enum ProcessJobStage {
    NONE("Sin etapa"),
    PREPARING_WORKSPACE("Preparando carpeta de trabajo"),
    PREPARING_ENGINE("Preparando motor"),
    RUNNING_ENGINE("Ejecutando motor"),
    WRITING_ARTIFACTS("Escribiendo artefactos"),
    EXPORT_READY("Salida lista"),
    CANCELLED("Cancelado"),
    FAILED("Fallido");

    private final String displayName;

    ProcessJobStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
