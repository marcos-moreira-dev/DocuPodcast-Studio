package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Precomputed summary used by the Document workspace and diagnostics panel. */
public record DocumentStructureSummary(
        int totalBlocks,
        int narratableBlocks,
        int structuralBlocks,
        int headings,
        int subheadings,
        int paragraphs,
        int listItems,
        int images,
        int tables,
        int warnings,
        int errors,
        Map<DocumentBlockType, Integer> countByType
) {
    public DocumentStructureSummary {
        countByType = countByType == null ? Map.of() : Map.copyOf(countByType);
    }

    public static DocumentStructureSummary from(ReadableDocument document) {
        Objects.requireNonNull(document, "document");
        EnumMap<DocumentBlockType, Integer> counts = new EnumMap<>(DocumentBlockType.class);
        document.blocks().forEach(block -> counts.merge(block.type(), 1, Integer::sum));
        return new DocumentStructureSummary(
                document.blocks().size(),
                (int) document.narratableBlockCount(),
                (int) document.structuralBlockCount(),
                (int) document.headingCount(),
                (int) document.subheadingCount(),
                (int) document.paragraphCount(),
                (int) document.listItemCount(),
                (int) document.imageNoticeCount(),
                (int) document.tableNoticeCount(),
                (int) document.warningCount(),
                (int) document.errorCount(),
                counts
        );
    }

    public boolean needsReadingProfileReview() {
        return structuralBlocks == 0 && paragraphs > 4;
    }
}
