package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

/** Modules that make the Voices workspace behave like a small sober administration app. */
public enum VoiceModuleId {
    HOME("Inicio", "Voces registradas y estado general"),
    ENGINE("Configurar motor", "Motor activo, dispositivo y prueba corta"),
    MANAGE("Gestionar voces", "Crear, editar, grabar, importar y reemplazar emociones");

    private final String title;
    private final String description;

    VoiceModuleId(String title, String description) {
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
