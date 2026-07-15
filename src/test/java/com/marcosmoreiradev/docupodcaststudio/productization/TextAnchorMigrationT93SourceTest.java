package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextAnchorMigrationT93SourceTest {
    @Test
    void projectFormatKeepsAnchorsAndAddsStudyWithoutDroppingLegacyRanges() throws Exception {
        String format = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectFormat.java"));
        String assignment = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/assignment/NarrativeLayerAssignment.java"));
        String writer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonWriter.java"));
        String reader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java"));

        assertTrue(format.contains("CURRENT_FORMAT_VERSION = 3"));
        assertTrue(assignment.contains("TextAnchor textAnchor"));
        assertTrue(assignment.contains("TextAnchor.legacy"));
        assertTrue(writer.contains("textAnchor"));
        assertTrue(writer.contains("writeStudy"));
        assertTrue(reader.contains("readTextAnchor"));
        assertTrue(reader.contains("readStudy"));
        assertTrue(reader.contains("TextAnchor.legacy"));
        assertTrue(writer.contains("sourceBlockId"));
    }
}
