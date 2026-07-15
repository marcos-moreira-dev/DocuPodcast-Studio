package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Summary shown before applying a Reading Profile to an imported document. */
public record ReadingProfilePreview(List<ReadingProfilePreviewItem> items) {
    public ReadingProfilePreview {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public long changedCount() {
        return items.stream().filter(ReadingProfilePreviewItem::changed).count();
    }

    public long manualOverrideCount() {
        return items.stream().filter(ReadingProfilePreviewItem::manualOverride).count();
    }

    public long proposedCount(DocumentBlockType type) {
        return items.stream().filter(item -> item.proposedType() == type).count();
    }

    public Map<DocumentBlockType, Long> proposedTypeCounts() {
        return items.stream().collect(Collectors.groupingBy(ReadingProfilePreviewItem::proposedType, Collectors.counting()));
    }

    public List<ReadingProfilePreviewItem> changedItems() {
        return items.stream().filter(ReadingProfilePreviewItem::changed).toList();
    }
}
