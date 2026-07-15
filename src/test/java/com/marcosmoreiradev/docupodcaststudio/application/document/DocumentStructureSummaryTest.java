package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentStructureSummaryTest {
    @Test
    void detectsNeedForReadingProfileReviewWhenWordHasNoStructure() {
        ReadableDocument document = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.DOCX,
                Path.of("notas.docx"),
                List.of(
                        DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "Uno", ""),
                        DocumentBlock.of("B0002", DocumentBlockType.PARAGRAPH, "Dos", ""),
                        DocumentBlock.of("B0003", DocumentBlockType.PARAGRAPH, "Tres", ""),
                        DocumentBlock.of("B0004", DocumentBlockType.PARAGRAPH, "Cuatro", ""),
                        DocumentBlock.of("B0005", DocumentBlockType.PARAGRAPH, "Cinco", "")
                )
        );

        DocumentStructureSummary summary = DocumentStructureSummary.from(document);

        assertEquals(5, summary.totalBlocks());
        assertTrue(summary.needsReadingProfileReview());
    }
}
