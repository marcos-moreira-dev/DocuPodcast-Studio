package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentInteractionProjectionAdapterTest {
    @Test
    void wordWithoutSelectionNormalizesPartialScopeToFullDocument() {
        ReadableDocument word = new ReadableDocument("Word", SourceDocumentFormat.DOCX,
                Path.of("word.docx"), List.of());

        DocumentInteractionProjection projection =
                new WordDocumentInteractionProjectionAdapter().project(
                        word, "", "SEG-CACHED", DocumentProcessingScope.FROM_SELECTION,
                        true, false);

        assertEquals(DocumentInteractionProjection.SourceKind.WORD,
                projection.sourceKind());
        assertFalse(projection.selectionValid());
        assertEquals("", projection.preferredNarrationSegmentId());
        assertEquals(DocumentProcessingScope.FULL_DOCUMENT,
                projection.effectiveScope());
    }

    @Test
    void wordAndPdfKeepIndependentCanonicalSelections() {
        ReadableDocument word = new ReadableDocument("Word", SourceDocumentFormat.DOCX,
                Path.of("word.docx"), List.of());
        DocumentInteractionProjection wordProjection =
                new WordDocumentInteractionProjectionAdapter().project(
                        word, "B0001", "SEG-WORD",
                        DocumentProcessingScope.FROM_SELECTION, true, true);

        PreparedPdfSource pdf = new PreparedPdfSource(new PreparedPdfWorkspaceRef(
                Path.of("pdf-workspace"), Path.of("source.pdf"), "a".repeat(64)), "PDF");
        DocumentInteractionProjection pdfProjection =
                new PdfDocumentInteractionProjectionAdapter().project(
                        pdf, new PdfRegionSelectionRef(2, "REG-2", 0, 8), "SEG-PDF",
                        DocumentProcessingScope.SINGLE_FRAGMENT, true, false);

        assertEquals(DocumentInteractionProjection.SourceKind.WORD,
                wordProjection.sourceKind());
        assertEquals("SEG-WORD", wordProjection.preferredNarrationSegmentId());
        assertEquals(DocumentInteractionProjection.SourceKind.PDF,
                pdfProjection.sourceKind());
        assertEquals("SEG-PDF", pdfProjection.preferredNarrationSegmentId());
        assertEquals(DocumentProcessingScope.SINGLE_FRAGMENT,
                pdfProjection.effectiveScope());
        assertTrue(wordProjection.supports(DocumentProcessingScope.INTERVAL));
        assertTrue(pdfProjection.supports(DocumentProcessingScope.INTERVAL));
        assertTrue(wordProjection.selectable(DocumentProcessingScope.FROM_SELECTION));
    }

    @Test
    void wordKeepsBlockIntervalWhileSelectionScopesNeedSelection() {
        ReadableDocument word = new ReadableDocument("Word", SourceDocumentFormat.DOCX,
                Path.of("word.docx"), List.of());

        DocumentInteractionProjection projection =
                new WordDocumentInteractionProjectionAdapter().project(
                        word, "", "", DocumentProcessingScope.INTERVAL,
                        true, false);

        assertEquals(DocumentProcessingScope.INTERVAL,
                projection.effectiveScope());
        assertTrue(projection.selectable(DocumentProcessingScope.INTERVAL));
        assertFalse(projection.selectable(DocumentProcessingScope.FROM_SELECTION));
        assertTrue(projection.selectable(DocumentProcessingScope.FULL_DOCUMENT));
    }
}
