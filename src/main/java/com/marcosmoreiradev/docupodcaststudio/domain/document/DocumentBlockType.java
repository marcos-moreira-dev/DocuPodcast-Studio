package com.marcosmoreiradev.docupodcaststudio.domain.document;

/** Coarse block kind extracted from an imported document. */
public enum DocumentBlockType {
    TITLE("Título principal"),
    HEADING("Título"),
    SUBHEADING("Subtítulo"),
    PARAGRAPH("Párrafo"),
    LIST_ITEM("Lista"),
    TABLE_NOTICE("Tabla"),
    IMAGE_NOTICE("Imagen"),
    MATH_NOTICE("Matemática/fórmula"),
    IGNORED("Ignorado"),
    EMPTY("Vacío");

    private final String displayName;

    DocumentBlockType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean narratableByDefault() {
        return this != EMPTY && this != IGNORED && !sourceVisual();
    }

    public boolean sourceVisual() {
        return this == IMAGE_NOTICE || this == TABLE_NOTICE || this == MATH_NOTICE;
    }

    public boolean structural() {
        return this == TITLE || this == HEADING || this == SUBHEADING;
    }
}
