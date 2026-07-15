package com.marcosmoreiradev.docupodcaststudio.presentation.components;

/**
 * Product icon catalog backed by PNG resources.
 *
 * <p>JavaFX renders these consistently in the ribbon and side rails. Keeping the
 * mapping here avoids scattering glyphs or ad-hoc image paths across views.</p>
 */
public enum AppIcon {
    OPEN_SOURCE("open-source.png", "Abrir fuente"),
    LISTEN("listen.png", "Escuchar"),
    NEW_PROJECT("new-project.png", "Nuevo proyecto"),
    OPEN_PROJECT("open-project.png", "Abrir proyecto"),
    SAVE("save.png", "Guardar"),
    PREPARE("prepare.png", "Preparar"),
    GENERATE_AUDIO("generate-audio.png", "Generar audio"),
    CANCEL("cancel.png", "Cancelar"),
    VOICE("voice.png", "Voces"),
    UPLOAD("upload.png", "Importar"),
    RAIL("rail.png", "Rail visual"),
    INDEX_TREE("tree-index.png", "Índice"),
    STORYBOARD("storyboard.png", "Visuales"),
    FULLSCREEN("fullscreen.png", "Pantalla completa"),
    SETTINGS("settings.png", "Configuración"),
    HELP("help.png", "Ayuda"),
    EXAMPLES("examples.png", "Ejemplos"),
    WAV("wav.png", "Audio WAV"),
    BUNDLE("bundle.png", "Paquete"),
    VIDEO("video.png", "Video"),
    CHECK("check.png", "Revisar"),
    FOLDER("folder.png", "Carpeta"),
    TEXT("text.png", "Texto"),
    AUDIO("audio.png", "Audio"),
    IMAGE("image.png", "Imagen"),
    COLLAPSE("collapse.png", "Ocultar"),
    EXPAND("expand.png", "Mostrar"),
    PAUSE("pause.png", "Pausar"),
    RESUME("resume.png", "Reanudar"),
    STOP("stop.png", "Detener"),
    PREVIOUS_FRAGMENT("previous-fragment.png", "Fragmento anterior"),
    NEXT_FRAGMENT("next-fragment.png", "Siguiente fragmento"),
    REFRESH("refresh.png", "Refrescar"),
    DEFAULT("help.png", "Acción");

    private final String fileName;
    private final String accessibleText;

    AppIcon(String fileName, String accessibleText) {
        this.fileName = fileName;
        this.accessibleText = accessibleText;
    }

    public String resourcePath() {
        return "/icons/ui/" + fileName;
    }

    public String accessibleText() {
        return accessibleText;
    }
}
