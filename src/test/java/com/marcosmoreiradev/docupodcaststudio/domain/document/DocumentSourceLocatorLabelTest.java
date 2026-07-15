package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSourceLocatorLabelTest {
    @Test
    void lineBasedDocumentsExposeLineLocator() {
        DocumentBlock block = DocumentBlock.of("B0002", DocumentBlockType.PARAGRAPH, "Texto", "TXT",
                Map.of("sourceLineStart", "4", "sourceLineEnd", "6"));

        assertTrue(block.sourceLocatorLabel(SourceDocumentFormat.TXT).contains("Líneas 4–6"));
    }
}
