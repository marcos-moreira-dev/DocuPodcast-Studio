package com.marcosmoreiradev.docupodcaststudio.domain.script;

/** Coarse segment kind produced from an imported document block. */
public enum NarrationSegmentType {
    TITLE("Título principal"),
    HEADING("Título"),
    SUBHEADING("Subtítulo"),
    PARAGRAPH("Párrafo"),
    LIST_ITEM("Lista"),
    TABLE_NOTICE("Aviso de tabla"),
    IMAGE_NOTICE("Aviso de imagen");

    private final String displayName;

    NarrationSegmentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
