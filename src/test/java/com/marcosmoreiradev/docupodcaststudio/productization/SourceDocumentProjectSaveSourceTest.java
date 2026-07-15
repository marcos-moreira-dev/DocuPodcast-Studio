package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceDocumentProjectSaveSourceTest {
    @Test
    void openingSourceWithoutProjectPromptsProjectSaveBesideSource() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(shell.contains("promptSaveProjectForImportedSourceIfNeeded(sourceFile)"));
        assertTrue(shell.contains("viewModel.currentProjectFile().isPresent()"));
        assertTrue(shell.contains("defaultProjectFileForSource"));
        assertTrue(shell.contains("setInitialDirectory(folder.toFile())"));
        assertTrue(shell.contains(".docupodcast.json"));
    }
}
