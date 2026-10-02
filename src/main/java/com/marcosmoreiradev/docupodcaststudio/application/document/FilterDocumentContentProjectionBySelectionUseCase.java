package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Keeps documentary visuals inside the same resolved scope as spoken narration. */
public final class FilterDocumentContentProjectionBySelectionUseCase {
    public DocumentContentProjection execute(DocumentContentProjection source,
                                             ResolvedDocumentProcessingSelection selection) {
        Objects.requireNonNull(source, "document content projection");
        Objects.requireNonNull(selection, "processing selection");
        if (selection.scope() == DocumentProcessingScope.FULL_DOCUMENT) return source;
        Set<String> segments = new HashSet<>(selection.resolvedSegmentIds());
        Set<Integer> pages = new HashSet<>(selection.resolvedPageNumbers());
        List<DocumentContentItem> selected = source.items().stream()
                .filter(item -> included(item, selection.scope(), segments, pages))
                .toList();
        return new DocumentContentProjection(source.title(), source.format(),
                source.sourcePath(), selected);
    }

    private static boolean included(DocumentContentItem item,
                                    DocumentProcessingScope scope,
                                    Set<String> segments,
                                    Set<Integer> pages) {
        boolean segmentMatch = item.narrationSegmentIds().stream().anyMatch(segments::contains);
        if (scope == DocumentProcessingScope.SINGLE_FRAGMENT) return segmentMatch;
        if (item.pdfAnchor().isPresent()) {
            return pages.contains(item.pdfAnchor().orElseThrow().pageNumber());
        }
        return segmentMatch;
    }
}
