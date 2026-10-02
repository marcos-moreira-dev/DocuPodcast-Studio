package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FilterPdfNarrationByPageIntervalUseCaseTest {
    @Test
    void keepsOnlySegmentsBoundToRequestedPages() {
        NarrationScriptDocument filtered = new FilterPdfNarrationByPageIntervalUseCase()
                .execute(script(5), DocumentProcessingInterval.pages(2, 3, 5));

        assertEquals(List.of("SEG-2", "SEG-3"), filtered.segments().stream()
                .map(NarrationSegment::id).toList());
    }

    private static NarrationScriptDocument script(int pages) {
        List<NarrationSegment> segments = IntStream.rangeClosed(1, pages)
                .mapToObj(page -> new NarrationSegment("SEG-" + page,
                        NarrationSegmentType.PARAGRAPH, "Página " + page,
                        "Texto de la página " + page, List.of("P" + page),
                        "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                        Map.of("pdfSourcePage", Integer.toString(page))))
                .toList();
        return new NarrationScriptDocument("SCRIPT-PDF", "PDF", "es", "sample.pdf",
                segments, Instant.EPOCH, Instant.EPOCH, "");
    }
}
