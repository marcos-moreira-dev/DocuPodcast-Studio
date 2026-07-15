package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualLanguageTv1SourceTest {
    @Test
    void userFacingDocumentAndHelpLanguageUsesVisualesNotStoryboard() throws Exception {
        String shellView = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String shellVm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String sourceVisual = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java"));
        String help = Files.readString(Path.of("src/main/resources/help/topics/getting-started.md"))
                + Files.readString(Path.of("src/main/resources/help/topics/import-word-notes.md"));

        assertTrue(shellView.contains("Importar imagen para visuales"));
        assertTrue(shellVm.contains("Secuencia visual válida"));
        assertTrue(document.contains("secuencia visual"));
        assertTrue(sourceVisual.contains("secuencia visual"));
        assertTrue(help.contains("visuales"));
        assertFalse(shellView.contains("Importar imagen para storyboard"));
        assertFalse(shellVm.contains("Asociar imagen / storyboard"));
        assertFalse(document.contains("entra al storyboard"));
        assertFalse(sourceVisual.contains("storyboard automáticamente"));
        assertFalse(help.contains("storyboard ya asignados"));
    }
}
