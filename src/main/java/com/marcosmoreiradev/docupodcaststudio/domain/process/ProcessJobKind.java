package com.marcosmoreiradev.docupodcaststudio.domain.process;

/** Product-level families of long running local processes. */
public enum ProcessJobKind {
    TTS_AUDIO("Generación de voz", true),
    MEDIA_PREPARATION("Preparación de audio/video", false),
    ENGINE_SETUP("Preparación de motores", false),
    VIDEO_RENDER("Render de video", true),
    MANAGED_DOWNLOAD("Descarga administrada", false),
    VISUAL_GENERATION("Generacion visual", false),
    FINAL_EXPORT("Exportacion final", false);

    private final String displayName;
    private final boolean currentlyBackedByPersistentJobs;

    ProcessJobKind(String displayName, boolean currentlyBackedByPersistentJobs) {
        this.displayName = displayName;
        this.currentlyBackedByPersistentJobs = currentlyBackedByPersistentJobs;
    }

    public String displayName() {
        return displayName;
    }

    public boolean currentlyBackedByPersistentJobs() {
        return currentlyBackedByPersistentJobs;
    }
}
