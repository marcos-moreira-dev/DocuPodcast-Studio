package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingIntervalUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.util.List;
import java.util.Objects;

/** Immutable authority shared by narration, audio coverage and document export. */
public record ResolvedDocumentProcessingSelection(
        DocumentProcessingScope scope,
        DocumentProcessingIntervalUnit unit,
        int requestedStart,
        int requestedEnd,
        List<Integer> resolvedPageNumbers,
        List<String> resolvedSegmentIds,
        String sourceDocumentId,
        String sourceSha,
        String selectionRevision,
        NarrationScriptDocument narration
) {
    public ResolvedDocumentProcessingSelection {
        scope = Objects.requireNonNull(scope, "scope");
        resolvedPageNumbers = resolvedPageNumbers == null
                ? List.of() : List.copyOf(resolvedPageNumbers);
        resolvedSegmentIds = resolvedSegmentIds == null
                ? List.of() : List.copyOf(resolvedSegmentIds);
        sourceDocumentId = Objects.toString(sourceDocumentId, "").strip();
        sourceSha = Objects.toString(sourceSha, "").strip();
        selectionRevision = Objects.toString(selectionRevision, "").strip();
        narration = Objects.requireNonNull(narration, "narration");
    }
}
