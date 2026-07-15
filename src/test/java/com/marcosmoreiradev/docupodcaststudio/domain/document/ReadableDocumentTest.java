package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReadableDocumentTest {
    @Test
    void summarizesBlocksAndIssues() {
        ReadableDocument document = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.DOCX,
                Path.of("notas.docx"),
                List.of(
                        DocumentBlock.of("B0001", DocumentBlockType.HEADING, "Introducción", "Heading 1"),
                        DocumentBlock.of("B0002", DocumentBlockType.PARAGRAPH, "Texto", ""),
                        DocumentBlock.of("B0003", DocumentBlockType.IMAGE_NOTICE, "Imagen detectada", "")
                ),
                List.of(DocumentImportIssue.warning("B0003", "Imagen sin descripción"))
        );

        assertEquals(2, document.narratableBlockCount());
        assertEquals(1, document.structuralBlockCount());
        assertEquals(1, document.headingCount());
        assertEquals(1, document.imageNoticeCount());
        assertEquals(1, document.sourceVisualBlockCount());
        assertEquals(1, document.warningCount());
        assertEquals(0, document.errorCount());
    }
}
