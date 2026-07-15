package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfImporterPageCountT95SourceTest {
    @Test
    void pdfImporterPreservesTotalPageCountInBlockMetadata() throws Exception {
        String importer = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfDocumentImporter.java"));

        assertTrue(importer.contains("metadata.put(\"sourcePageCount\""));
        assertTrue(importer.contains("Integer.toString(extraction.pageCount())"));
        assertTrue(importer.contains("Integer.toString(safePages)"));
    }
}
