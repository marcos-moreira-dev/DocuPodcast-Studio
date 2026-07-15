package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.Objects;

/** Computed navigation tree for a readable document. It does not mutate the source document. */
public record DocumentOutlineProjection(
        DocumentOutlineOrigin origin,
        String title,
        String detail,
        List<DocumentOutlineEntry> entries,
        int indexedEntryCount,
        int flatLimit
) {
    public DocumentOutlineProjection {
        origin = Objects.requireNonNullElse(origin, DocumentOutlineOrigin.FLAT);
        title = normalize(title, "Indice del documento");
        detail = normalize(detail, "");
        entries = entries == null ? List.of() : List.copyOf(entries);
        indexedEntryCount = Math.max(0, indexedEntryCount);
        flatLimit = Math.max(0, flatLimit);
    }

    public boolean flat() {
        return origin == DocumentOutlineOrigin.FLAT;
    }

    public static DocumentOutlineProjection empty() {
        return new DocumentOutlineProjection(
                DocumentOutlineOrigin.FLAT,
                "Indice del documento",
                "Abre un Word/DOCX, Markdown, TXT o PDF para navegar por titulos, secciones o bloques detectados.",
                List.of(),
                0,
                0);
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
