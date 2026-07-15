package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadableDocumentDiagnosticsTest {
    @Test
    void exposesDocumentMetricsUsedByDocumentWorkspace() {
        ReadableDocument document = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.DOCX,
                Path.of("notas.docx"),
                List.of(
                        DocumentBlock.of("B0001", DocumentBlockType.HEADING, "Introducción", "Heading1"),
                        DocumentBlock.of("B0002", DocumentBlockType.PARAGRAPH, "Texto de prueba", ""),
                        DocumentBlock.of("B0003", DocumentBlockType.IMAGE_NOTICE, "Imagen detectada sin descripción.", "")
                ),
                List.of(DocumentImportIssue.warning("IMAGE_WITHOUT_DESCRIPTION", "Sin alt text", "B0003"))
        );

        assertEquals(1, document.headingCount());
        assertEquals(1, document.paragraphCount());
        assertEquals(1, document.imageNoticeCount());
        assertEquals(1, document.sourceVisualBlockCount());
        assertEquals(1, document.warningCount());
        assertEquals(4, document.wordCount());
    }
}
