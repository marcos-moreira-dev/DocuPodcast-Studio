package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** Whether DocuPodcast owns the file enough to delete it safely. */
public enum VoiceFileOwnership {
    APP_RESOURCE("Recurso protegido de la aplicación", false),
    USER_APPDATA("Archivo gestionado en datos del usuario", true),
    PROJECT_ASSET("Archivo gestionado dentro del proyecto", true),
    EXTERNAL_REFERENCE("Archivo externo referenciado", false);

    private final String displayName;
    private final boolean managedDeletable;

    VoiceFileOwnership(String displayName, boolean managedDeletable) {
        this.displayName = displayName;
        this.managedDeletable = managedDeletable;
    }

    public String displayName() {
        return displayName;
    }

    public boolean managedDeletable() {
        return managedDeletable;
    }
}
