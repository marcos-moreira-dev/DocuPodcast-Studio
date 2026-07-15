package com.marcosmoreiradev.docupodcaststudio.domain.document;

/** Source formats accepted by the document import pipeline. */
public enum SourceDocumentFormat {
    DOCX("Word/DOCX"),
    MARKDOWN("Markdown"),
    PDF("PDF"),
    TXT("Texto plano"),
    UNKNOWN("Desconocido");

    private final String displayName;

    SourceDocumentFormat(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
