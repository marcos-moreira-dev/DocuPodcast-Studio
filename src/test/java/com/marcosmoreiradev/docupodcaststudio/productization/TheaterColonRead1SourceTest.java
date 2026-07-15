package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** THEATER-COLON-READ1: optional theatre/dialogue reading from after ':' without mutating Word. */
final class TheaterColonRead1SourceTest {
    @Test
    void documentSidebarExposesOperationalCheckboxAndExample() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(panel.contains("Leer desde después de ':' (requiere reconstruir fragmentos de audio)"));
        assertTrue(panel.contains("readAfterColonForNarrationProperty"));
        assertTrue(viewModel.contains("setReadAfterColonForNarration"));
        assertTrue(viewModel.contains("Reconstruir fragmentos de audio aplicará el cambio"));
    }

    @Test
    void projectionBuilderReceivesReadAfterColonFlag() throws Exception {
        String builder = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/script/BuildNarrationScriptUseCase.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/script/ReadAfterColonTextPolicy.java"));
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentNarrationCoordinator.java"));
        assertTrue(builder.contains("build(ReadableDocument document, String language, boolean readAfterColon)"));
        assertTrue(builder.contains("ReadAfterColonTextPolicy.narrationText"));
        assertTrue(policy.contains("Vaquero: Voy a conquistar el viejo oeste"));
        assertTrue(coordinator.contains("buildNarrationProjection(ProjectSession session, ReadableDocument document, boolean readAfterColon)"));
    }
}
