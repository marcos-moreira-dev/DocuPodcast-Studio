package com.marcosmoreiradev.docupodcaststudio.application.document;

/**
 * Product-facing state for the document listening journey.
 *
 * <p>This lives in application so presentation can render the same end-to-end truth without
 * duplicating precondition logic in JavaFX view models.</p>
 */
public record ListeningSessionState(String title, String detail, String styleClass) {
    private static final String BASE_STYLE = "document-listen-flow-status";

    public static ListeningSessionState noDocument() {
        return new ListeningSessionState(
                "1. Abre un Word/DOCX",
                "Carga un documento para leerlo como página y escucharlo desde esta pantalla.",
                BASE_STYLE + " document-listen-flow-waiting");
    }

    public static ListeningSessionState notNarratable() {
        return new ListeningSessionState(
                "Documento sin texto narrable",
                "Revisa el perfil de lectura o reclasifica bloques antes de escuchar.",
                BASE_STYLE + " document-listen-flow-warning");
    }

    public static ListeningSessionState projectionPending() {
        return new ListeningSessionState(
                "2. Lectura preparada al escuchar",
                "Pulsa Escuchar documento: DocuPodcast preparará segmentos internos sin modificar la fuente original.",
                BASE_STYLE + " document-listen-flow-ready");
    }

    public static ListeningSessionState saveRequired() {
        return new ListeningSessionState(
                "3. Guarda el proyecto para generar audio",
                "El documento ya puede convertirse en audio, pero primero se necesita una carpeta de proyecto para guardar fragmentos de audio y evidencias.",
                BASE_STYLE + " document-listen-flow-warning");
    }

    public static ListeningSessionState audioPending() {
        return new ListeningSessionState(
                "3. Audio listo al escuchar",
                "Pulsa Escuchar documento: se generará audio por segmentos y se iniciará cuando haya buffer suficiente.",
                BASE_STYLE + " document-listen-flow-ready");
    }

    public static ListeningSessionState buffering() {
        return new ListeningSessionState(
                "Preparando audio por fragmentos",
                "Puedes permanecer en Documento: la app generará fragmentos y empezará a reproducir cuando haya buffer.",
                BASE_STYLE + " document-listen-flow-active");
    }

    public static ListeningSessionState playable() {
        return new ListeningSessionState(
                "Listo para reproducir",
                "El audio ya tiene manifest: usa Escuchar documento o selecciona una oración para reproducir desde ahí.",
                BASE_STYLE + " document-listen-flow-complete");
    }

    public static ListeningSessionState from(DocumentListenPlan plan) {
        if (plan == null) {
            return noDocument();
        }
        return switch (plan.phase()) {
            case NO_DOCUMENT -> noDocument();
            case NO_NARRATABLE_TEXT -> notNarratable();
            case BUILD_NARRATION_PROJECTION -> projectionPending();
            case PLAY_EXISTING_AUDIO -> playable();
            case WAIT_FOR_AUDIO_BUFFER -> buffering();
            case SAVE_PROJECT_REQUIRED -> saveRequired();
            case GENERATE_AUDIO -> audioPending();
        };
    }
}
