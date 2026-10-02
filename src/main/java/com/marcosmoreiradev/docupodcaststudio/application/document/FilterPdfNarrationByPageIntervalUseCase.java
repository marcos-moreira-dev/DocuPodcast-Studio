package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingIntervalUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.List;
import java.util.Objects;

/** Restricts a prepared PDF narration to segments bound to an explicit page interval. */
public final class FilterPdfNarrationByPageIntervalUseCase {
    public NarrationScriptDocument execute(NarrationScriptDocument script,
                                           DocumentProcessingInterval interval) {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(interval, "interval");
        if (interval.unit() != DocumentProcessingIntervalUnit.PAGE) {
            throw new IllegalArgumentException("PDF narration requires a PAGE interval");
        }
        List<NarrationSegment> selected = script.segments().stream()
                .filter(segment -> {
                    int page = sourcePage(segment);
                    return page >= interval.start() && page <= interval.end();
                })
                .toList();
        return new NarrationScriptDocument(script.id(), script.title(), script.language(),
                script.sourceDocumentTitle(), selected, script.createdAt(), script.updatedAt(),
                script.notes());
    }

    private static int sourcePage(NarrationSegment segment) {
        try {
            return Integer.parseInt(segment.metadata().getOrDefault("pdfSourcePage", "0"));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }
}
