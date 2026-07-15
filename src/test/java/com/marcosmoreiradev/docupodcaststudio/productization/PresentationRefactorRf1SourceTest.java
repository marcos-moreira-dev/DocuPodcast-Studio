package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PresentationRefactorRf1SourceTest {
    @Test
    void exportWorkflowIsExtractedFromShellViewModel() throws IOException {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java");
        assertTrue(coordinator.contains("final class ExportWorkflowCoordinator"));
        assertTrue(coordinator.contains("exportPodcastWav"));
        assertTrue(coordinator.contains("exportSimpleVideoPackage"));
        assertTrue(coordinator.contains("ProjectSaveCallback"));

        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertTrue(viewModel.contains("ExportWorkflowCoordinator"));
        assertTrue(viewModel.contains("exportWorkflow.exportProjectBundle"));
        assertTrue(viewModel.contains("exportWorkflow.exportSimpleVideoPackage"));
    }


    @Test
    void readingComfortWorkflowIsExtractedFromShellViewModel() throws IOException {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ReadingComfortCoordinator.java");
        assertTrue(coordinator.contains("final class ReadingComfortCoordinator"));
        assertTrue(coordinator.contains("loadInitialFontSize"));
        assertTrue(coordinator.contains("persistFontSize"));
        assertTrue(coordinator.contains("zoomPercent"));
        assertTrue(coordinator.contains("MIN_FONT_SIZE"));

        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertTrue(viewModel.contains("ReadingComfortCoordinator"));
        assertTrue(viewModel.contains("readingComfortWorkflow.loadInitialFontSize"));
        assertTrue(viewModel.contains("readingComfortWorkflow.persistFontSize"));
        assertTrue(!viewModel.contains("OperationalSettingsValidationReport"), "la validación de settings ya no debe vivir en el ViewModel");
        assertTrue(!viewModel.contains("new OperationalSettings.ReadingDocumentSettings"), "la construcción de settings de lectura debe vivir en el coordinador");
    }

    @Test
    void documentationRecordsRf1AsFirstPresentationRefactorStep() throws IOException {
        String docs = read("docs/productizacion/RF1_DIVIDIR_SHELL_VIEW_MODEL.md");
        assertTrue(docs.contains("RF1"));
        assertTrue(docs.contains("DocuPodcastShellViewModel"));
        assertTrue(docs.contains("ExportWorkflowCoordinator"));
        assertTrue(docs.contains("ReadingComfortCoordinator"));
        assertTrue(docs.contains("no cambia comportamiento visible"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
