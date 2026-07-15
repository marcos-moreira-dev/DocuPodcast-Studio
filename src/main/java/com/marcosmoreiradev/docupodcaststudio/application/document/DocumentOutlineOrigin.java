package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Source used to build the navigable document outline shown in the side dock. */
public enum DocumentOutlineOrigin {
    PDF_BOOKMARKS("bookmarks PDF"),
    CONTENTS("tabla de contenidos"),
    HEADINGS("titulos detectados"),
    INFERRED_SECTIONS("secciones inferidas"),
    PDF_PAGES("Paginas del PDF"),
    FLAT("navegacion plana");

    private final String label;

    DocumentOutlineOrigin(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
