package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfVisiblePageAudioStartSelectorTest {
    @Test
    void selectsFirstNarratableSegmentAtOrAfterVisiblePdfPage() {
        ReadableDocument document = new ReadableDocument("PDF", SourceDocumentFormat.PDF, Path.of("book.pdf"), List.of(
                block("B1", 2),
                block("B2", 8),
                block("B3", 9)));
        NarrationScriptDocument script = NarrationScriptDocument.create("Lectura", "es", "PDF", List.of(
                segment("S1", "B1"),
                segment("S2", "B2"),
                segment("S3", "B3")));

        var selected = PdfVisiblePageAudioStartSelector.firstSegmentAtOrAfterVisiblePage(document, script, 7);

        assertTrue(selected.isPresent());
        assertEquals("S2", selected.get().id());
    }

    @Test
    void ignoresNonPdfDocuments() {
        ReadableDocument document = new ReadableDocument("Word", SourceDocumentFormat.DOCX, Path.of("doc.docx"),
                List.of(block("B1", 1)));
        NarrationScriptDocument script = NarrationScriptDocument.create("Lectura", "es", "Word", List.of(segment("S1", "B1")));

        assertTrue(PdfVisiblePageAudioStartSelector.firstSegmentAtOrAfterVisiblePage(document, script, 1).isEmpty());
    }

    private static DocumentBlock block(String id, int page) {
        return DocumentBlock.of(id, DocumentBlockType.PARAGRAPH, "Texto narrable " + page, "",
                Map.of("sourcePage", Integer.toString(page), "ocr", "true", "extractionMode", "ocr-local"));
    }

    private static NarrationSegment segment(String id, String blockId) {
        return new NarrationSegment(id, NarrationSegmentType.PARAGRAPH, "", "Texto narrable", List.of(blockId),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL", Map.of());
    }
}
