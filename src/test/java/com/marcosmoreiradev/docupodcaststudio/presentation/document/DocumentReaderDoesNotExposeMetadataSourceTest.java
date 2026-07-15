package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T81B guardrail: the central reader must be clean; technical metadata belongs in side panels. */
final class DocumentReaderDoesNotExposeMetadataSourceTest {
    @Test
    void documentWorkspaceDoesNotDumpImporterMetadataIntoTheReadingPage() throws Exception {
        String reader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertFalse(reader.contains("document-block-metadata"));
        assertFalse(reader.contains("metadataSummary("));
        assertFalse(reader.contains("styleId:"));
        assertFalse(reader.contains("styleName:"));
        assertFalse(reader.contains("readingProfile:"));
        assertFalse(reader.contains("classificationSource:"));
    }

    @Test
    void technicalMetadataStillLivesInThePropertiesPanel() throws Exception {
        String properties = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentPropertiesPanel.java"));

        assertTrue(properties.contains("Metadatos"));
        assertTrue(properties.contains("selected.metadata()"));
        assertTrue(properties.contains("selected.metadata().forEach"));
    }
}
