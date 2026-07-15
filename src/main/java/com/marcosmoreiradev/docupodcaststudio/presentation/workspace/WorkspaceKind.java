package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

/** Workspaces planned for DocuPodcast Studio. */
public enum WorkspaceKind {
    WELCOME_HOME("Inicio"),
    DOCUMENT_READER("Documento"),
    THEATRE_SCRIPT("Guión"),
    SCRIPT_EDITOR("Interno"),
    STORYBOARD("Visual interno"),
    AUDIO_JOBS("Procesos internos"),
    VOICE_LIBRARY("Voces"),
    NARRATIVE_VISUAL_PRODUCTION("Visual narrativa"),
    THEATRE_IMAGE_GENERATION("Gestionar frames de la obra"),
    OBSERVABILITY("Observabilidad"),
    SETTINGS("Configuración");

    private final String displayName;

    WorkspaceKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
