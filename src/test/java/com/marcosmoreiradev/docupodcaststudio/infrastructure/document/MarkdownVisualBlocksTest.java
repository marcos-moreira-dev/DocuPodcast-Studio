package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarkdownVisualBlocksTest {
    @Test
    void tablesAndMathAreSourceVisualNotNarratable() throws Exception {
        Path file = Files.createTempFile("docupodcast-md-visual", ".md");
        Files.writeString(file, "# Reporte\n\n| Cuenta | Valor |\n| --- | --- |\n| Caja | 100 |\n\n$$x^2 + y^2 = z^2$$\n");

        var document = new MarkdownDocumentImporter().importDocument(file);

        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.TABLE_NOTICE && !block.narratable()));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.MATH_NOTICE && !block.narratable()));
    }
}
