package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** Stage reached by the managed image-engine smoke test. */
public enum ImageEngineSmokeStage {
    VERIFYING_RUNTIME("Verificando runtime"),
    VERIFYING_MODEL("Verificando modelo"),
    STARTING_ENGINE("Iniciando motor"),
    GENERATING_TEST("Generando prueba"),
    COMPLETE("Prueba completada"),
    FAILED("Prueba fallida");

    private final String displayName;

    ImageEngineSmokeStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
