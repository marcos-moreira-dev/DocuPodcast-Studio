package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds an outline directly from prepared PDF V2 heading regions. */
public final class BuildPdfEnhancedOutlineUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final PdfBookmarkOutlineReader bookmarks;

    public BuildPdfEnhancedOutlineUseCase(PreparedPdfDocumentRepository repository) {
        this(repository, PdfBookmarkOutlineReader.NONE);
    }

    public BuildPdfEnhancedOutlineUseCase(PreparedPdfDocumentRepository repository,
                                          PdfBookmarkOutlineReader bookmarks) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.bookmarks = Objects.requireNonNull(bookmarks, "bookmarks");
    }

    public DocumentOutlineProjection build(PreparedPdfWorkspaceRef workspace) {
        if (workspace == null) return empty();
        try {
            ArrayList<DocumentOutlineEntry> entries = new ArrayList<>();
            int index = 0;
            for (DocumentOutlineHint bookmark : bookmarks.hintsFor(workspace.sourcePath())) {
                entries.add(new DocumentOutlineEntry("pdf-bookmark-" + index,
                        "pdf-page-" + bookmark.sourcePage(), DocumentBlockType.HEADING,
                        bookmark.title(), bookmark.sourcePage(), index++, List.of()));
            }
            for (var entry : repository.loadRegionIndex(workspace.projectRoot()).headings()) {
                var region = entry.region();
                PdfRegionType type = region.effectiveType();
                DocumentBlockType blockType = switch (type) {
                    case TITLE -> DocumentBlockType.TITLE;
                    case HEADING -> DocumentBlockType.HEADING;
                    default -> DocumentBlockType.SUBHEADING;
                };
                entries.add(new DocumentOutlineEntry("pdf-outline-" + region.id(), region.id(),
                        blockType, region.effectiveText(), Integer.toString(entry.pageNumber()),
                        index++, List.of()));
            }
            if (entries.isEmpty()) return empty();
            DocumentOutlineOrigin origin = entries.stream()
                    .anyMatch(entry -> entry.id().startsWith("pdf-bookmark-"))
                    ? DocumentOutlineOrigin.PDF_BOOKMARKS : DocumentOutlineOrigin.HEADINGS;
            return new DocumentOutlineProjection(origin,
                    "Índice del PDF preparado",
                    origin == DocumentOutlineOrigin.PDF_BOOKMARKS
                            ? "Bookmarks del PDF y secciones preparadas."
                            : "Títulos y secciones leídos directamente desde regiones PDF V2.",
                    entries, entries.size(), 0);
        } catch (IOException ex) {
            return new DocumentOutlineProjection(DocumentOutlineOrigin.FLAT,
                    "Índice del PDF preparado",
                    "No se pudo leer el índice V2: " + ex.getMessage(),
                    List.of(), 0, 0);
        }
    }

    private static DocumentOutlineProjection empty() {
        return new DocumentOutlineProjection(DocumentOutlineOrigin.FLAT,
                "Índice del PDF preparado",
                "Todavía no hay títulos o secciones en las páginas preparadas.",
                List.of(), 0, 0);
    }
}
