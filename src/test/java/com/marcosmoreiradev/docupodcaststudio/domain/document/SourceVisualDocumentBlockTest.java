package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVisualDocumentBlockTest {
    @Test
    void imagesTablesAndMathAreSourceVisualBlocksNotNarratedByDefault() {
        assertTrue(DocumentBlockType.IMAGE_NOTICE.sourceVisual());
        assertTrue(DocumentBlockType.TABLE_NOTICE.sourceVisual());
        assertTrue(DocumentBlockType.MATH_NOTICE.sourceVisual());

        assertFalse(DocumentBlock.of("B001", DocumentBlockType.IMAGE_NOTICE, "Imagen detectada", "").narratable());
        assertFalse(DocumentBlock.of("B002", DocumentBlockType.TABLE_NOTICE, "Tabla detectada", "").narratable());
        assertFalse(DocumentBlock.of("B003", DocumentBlockType.MATH_NOTICE, "Fórmula detectada", "").narratable());
    }
}
