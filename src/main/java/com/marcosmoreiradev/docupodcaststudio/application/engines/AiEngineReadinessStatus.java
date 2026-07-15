package com.marcosmoreiradev.docupodcaststudio.application.engines;

/** Coarse readiness state shown by the technical Settings/Diagnostics surfaces. */
public enum AiEngineReadinessStatus {
    READY("Listo"),
    READY_WITH_WARNINGS("Listo con advertencias"),
    NEEDS_VERIFICATION("Requiere prueba"),
    NEEDS_CONFIGURATION("Requiere configuración"),
    MISSING_RUNTIME("Falta ejecutable o wrapper"),
    MISSING_MODEL("Falta modelo"),
    OPTIONAL_NOT_CONFIGURED("Opcional no configurado");

    private final String label;

    AiEngineReadinessStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean usable() {
        return this == READY || this == READY_WITH_WARNINGS;
    }

    public boolean blocksRealAiDemo() {
        return this == NEEDS_CONFIGURATION || this == NEEDS_VERIFICATION || this == MISSING_RUNTIME || this == MISSING_MODEL;
    }
}
