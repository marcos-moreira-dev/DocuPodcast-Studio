package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExecutableBrainAuditSourceTest {
    @Test
    void t60DocumentsExecutableBrainAuditCriteriaAndCurrentDebts() throws Exception {
        String audit = read("docs/productizacion/AUDITORIA_EJECUTABLE_CEREBRO_T60.md");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T60_CEREBRO_DOCUMENTO_RAIZ.md");
        String brain = read("docs/productizacion/MAPA_CEREBRO_APP.md");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(audit.contains("El Documento narrable es el objeto padre de la experiencia V1"));
        assertTrue(audit.contains("ViewModel gigante"));
        assertTrue(audit.contains("Navegación dirty"));
        assertTrue(audit.contains("Capas con placeholders"));
        assertTrue(audit.contains("StoryBoard") || audit.contains("Storyboard"));
        assertTrue(audit.contains("Video honesto"));
        assertTrue(audit.contains("Settings operativas"));

        assertTrue(roadmap.contains("DocumentIntakeCoordinator"));
        assertTrue(roadmap.contains("NarratedDocumentCoordinator"));
        assertTrue(roadmap.contains("NarrationProjectionCoordinator"));
        assertTrue(roadmap.contains("PlaybackWorkflowCoordinator"));
        assertTrue(roadmap.contains("SettingsWorkflowCoordinator"));

        assertTrue(brain.contains("Ajuste T60 — Documento narrable raíz"));
        assertTrue(viewModel.contains("withViewState"),
                "La deuda actual de navegación/dirty debe seguir visible hasta el refactor T61.");
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
