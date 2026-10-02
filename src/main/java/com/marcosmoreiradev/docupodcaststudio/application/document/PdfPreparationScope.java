package com.marcosmoreiradev.docupodcaststudio.application.document;

/** User-meaningful scope for preparing PDF pages. */
public enum PdfPreparationScope {
    CURRENT_PAGE("Página actual"),
    CURRENT_SECTION("Sección actual"),
    FROM_CURRENT_TO_SECTION_END("Desde aquí hasta el final de la sección"),
    PAGE_RANGE("Rango de páginas"),
    WHOLE_DOCUMENT("Documento completo");

    private final String displayName;

    PdfPreparationScope(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
