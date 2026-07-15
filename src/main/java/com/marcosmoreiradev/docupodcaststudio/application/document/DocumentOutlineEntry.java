package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;

import java.util.List;
import java.util.Objects;

/** One navigable entry in the computed document outline. */
public record DocumentOutlineEntry(
        String id,
        String blockId,
        DocumentBlockType kind,
        String label,
        String sourcePage,
        int sourceIndex,
        List<DocumentOutlineEntry> children
) {
    public DocumentOutlineEntry {
        id = normalizeRequired(id, "id");
        blockId = normalize(blockId);
        kind = Objects.requireNonNullElse(kind, DocumentBlockType.EMPTY);
        label = normalizeRequired(label, "label");
        sourcePage = normalize(sourcePage);
        sourceIndex = Math.max(-1, sourceIndex);
        children = children == null ? List.of() : List.copyOf(children);
    }

    public boolean hasBlock() {
        return !blockId.isBlank();
    }

    public DocumentOutlineEntry withChildren(List<DocumentOutlineEntry> nextChildren) {
        return new DocumentOutlineEntry(id, blockId, kind, label, sourcePage, sourceIndex, nextChildren);
    }

    public static DocumentOutlineEntry message(String id, String label) {
        return new DocumentOutlineEntry(id, "", DocumentBlockType.EMPTY, label, "", -1, List.of());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}
