package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentVisualBlocksT110RSourceTest {
    @Test
    void t110rDocumentsSourceVisualBlocksAndSilentStoryboardDuration() throws Exception {
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/DocxDocumentImporter.java"));
        String documentView = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettings.java"));
        String guide = Files.readString(Path.of("src/main/resources/help/topics/storyboard-live.md"));

        assertTrue(importer.contains("embeddedImageBase64"));
        assertTrue(importer.contains("visualBlock"));
        assertTrue(importer.contains("storyboardAssignment"));
        assertTrue(documentView.contains("sourceVisualPreview"));
        assertTrue(documentView.contains("SourceVisualBlockView"));
        assertTrue(settings.contains("silentVisualBlockSeconds"));
        assertTrue(guide.contains("No entra al video automáticamente"));
        assertTrue(guide.contains("Duración silenciosa por defecto: 5 segundos"));
    }
}
