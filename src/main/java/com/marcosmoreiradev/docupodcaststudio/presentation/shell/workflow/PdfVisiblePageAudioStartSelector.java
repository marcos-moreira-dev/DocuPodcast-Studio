package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.Optional;

/** Selects the first narratable PDF segment at or below the visible page. */
public final class PdfVisiblePageAudioStartSelector {
    private PdfVisiblePageAudioStartSelector() {
    }

    public static boolean isPdf(ReadableDocument document) {
        return document != null && document.format() == SourceDocumentFormat.PDF;
    }

    public static Optional<NarrationSegment> firstSegmentAtOrAfterVisiblePage(
            ReadableDocument document, NarrationScriptDocument script, int visiblePage) {
        if (!isPdf(document) || script == null || script.empty()) {
            return Optional.empty();
        }
        int pivot = Math.max(1, visiblePage);
        return script.segments().stream()
                .filter(NarrationSegment::narratable)
                .filter(segment -> sourcePageForSegment(document, segment) >= pivot)
                .findFirst();
    }

    private static int sourcePageForSegment(ReadableDocument document, NarrationSegment segment) {
        if (segment == null) {
            return 0;
        }
        return segment.sourceBlockIds().stream()
                .map(document::blockById)
                .flatMap(Optional::stream)
                .map(block -> block.metadata().getOrDefault("sourcePage", ""))
                .mapToInt(PdfVisiblePageAudioStartSelector::positiveIntOrZero)
                .filter(value -> value > 0)
                .findFirst()
                .orElse(0);
    }

    private static int positiveIntOrZero(String value) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.strip());
            return parsed > 0 ? parsed : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
