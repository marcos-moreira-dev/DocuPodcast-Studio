package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Ordered, format-neutral documentary projection. */
public record DocumentContentProjection(
        String title,
        SourceDocumentFormat format,
        Path sourcePath,
        List<DocumentContentItem> items
) {
    public DocumentContentProjection {
        title = title == null || title.isBlank() ? "Documento" : title.strip();
        format = Objects.requireNonNullElse(format, SourceDocumentFormat.UNKNOWN);
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").toAbsolutePath().normalize();
        items = items == null ? List.of() : List.copyOf(items);
        long unique = items.stream().map(DocumentContentItem::contentId).distinct().count();
        if (unique != items.size()) throw new IllegalArgumentException("contentId values must be unique");
    }

    public Optional<DocumentContentItem> itemById(String contentId) {
        if (contentId == null || contentId.isBlank()) return Optional.empty();
        String normalized = contentId.strip();
        return items.stream().filter(item -> item.matchesContentId(normalized)).findFirst();
    }

    public Optional<DocumentContentItem> itemForSegment(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) return Optional.empty();
        String normalized = segmentId.strip();
        return items.stream().filter(item -> item.narrationSegmentIds().contains(normalized)).findFirst();
    }
}
