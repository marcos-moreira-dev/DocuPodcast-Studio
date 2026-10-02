package com.marcosmoreiradev.docupodcaststudio.presentation.components;

/**
 * Product icon catalog backed by PNG resources.
 *
 * <p>JavaFX renders these consistently in the ribbon and side rails. Keeping the
 * mapping here avoids scattering glyphs or ad-hoc image paths across views.</p>
 */
public enum AppIcon {
    PRODUCT_SOURCE_DOCUMENT("/icons/product/source-document.png", "Fuente documental", true),
    PRODUCT_PROJECT_NEW("/icons/product/project-new.png", "Nuevo proyecto", true),
    PRODUCT_PROJECT_OPEN("/icons/product/project-open.png", "Abrir proyecto", true),
    PRODUCT_PROJECT_SAVE("/icons/product/project-save.png", "Guardar proyecto", true),
    PRODUCT_SETTINGS("/icons/product/settings.png", "Configuración", true),
    PRODUCT_GUIDE("/icons/product/guide.png", "Guía", true),
    PRODUCT_READING_PREPARE("/icons/product/reading-prepare.png", "Preparar lectura", true),
    PRODUCT_AUDIO_GENERATE("/icons/product/audio-generate.png", "Generar audio", true),
    PRODUCT_AUDIO_CANCEL("/icons/product/audio-cancel.png", "Cancelar audio", true),
    PRODUCT_HOME("/icons/product/home.png", "Inicio", true),
    PRODUCT_PLAYBAR_DOCK("/icons/product/playbar-dock.png", "Controles de lectura", true),
    PRODUCT_THEATRE_STRUCTURE("/icons/product/theatre-structure.png", "Estructura teatral", true),
    PRODUCT_THEATRE_GRAMMAR_IMPORT("/icons/product/theatre-grammar-import.png", "Importar gramática teatral", true),
    PRODUCT_THEATRE_GRAMMAR_TEMPLATE("/icons/product/theatre-grammar-template.png", "Plantilla teatral", true),
    PRODUCT_THEATRE_FRAME_STUDIO("/icons/product/theatre-frame-studio.png", "Frames de la obra", true),
    PRODUCT_EXAMPLE_PROJECT("/icons/product/example-project.png", "Proyecto de ejemplo", true),
    PRODUCT_DOCUMENT_FRAGMENT("/icons/product/document-fragment.png", "Fragmento documental", true),
    PRODUCT_DOCUMENT_INDEX_PREFERENCES("/icons/product/document-index-preferences.png", "Índices y preferencias", true),
    PRODUCT_DOCUMENT_AUDIO("/icons/product/document-audio.png", "Audio del documento", true),
    PRODUCT_DOCUMENTARY_VIDEO_CONTENT("/icons/product/documentary-video-content.png", "Contenido del video", true),
    PRODUCT_THEATRE_MEDIA_LAYERS("/icons/product/theatre-media-layers.png", "Capas multimedia", true),
    PRODUCT_THEATRE_CHARACTERS("/icons/product/theatre-characters.png", "Personajes", true),
    PRODUCT_THEATRE_TEXT_MAP("/icons/product/theatre-text-map.png", "Mapa textual", true),
    PRODUCT_THEATRE_SPATIAL_ACTIONS("/icons/product/theatre-spatial-actions.png", "Mapa espacial y acciones", true),
    PRODUCT_THEATRE_OBJECTS("/icons/product/theatre-objects.png", "Objetos teatrales", true),

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

    private final String resourcePath;
    private final String accessibleText;
    private final boolean productAsset;

    AppIcon(String fileName, String accessibleText) {
        this.resourcePath = "/icons/ui/" + fileName;
        this.accessibleText = accessibleText;
        this.productAsset = false;
    }

    AppIcon(String resourcePath, String accessibleText, boolean productAsset) {
        this.resourcePath = productAsset ? resourcePath : "/icons/ui/" + resourcePath;
        this.accessibleText = accessibleText;
        this.productAsset = productAsset;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public String accessibleText() {
        return accessibleText;
    }

    public boolean isProductAsset() {
        return productAsset;
    }
}
