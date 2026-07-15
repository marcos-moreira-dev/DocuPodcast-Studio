package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Modules in the theatrical local image-generation workspace. */
public enum TheatreAiModuleId {
    HOME("Inicio", "Estado del motor, cola y siguiente accion."),
    ENGINE("Configurar motor", "Motor local, preset, dispositivo y prueba corta."),
    GENERATE("Generar", "Contexto, prompt y alcance de generacion."),
    JOBS("Trabajos", "Cola, lotes, resultados, errores y aprobacion.");

    private final String title;
    private final String description;

    TheatreAiModuleId(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
