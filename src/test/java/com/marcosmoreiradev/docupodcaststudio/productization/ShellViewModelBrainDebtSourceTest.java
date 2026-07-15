package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellViewModelBrainDebtSourceTest {
    @Test
    void shellViewModelDebtIsCappedAndCoordinatorRoadmapIsExplicit() throws IOException {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        long lineCount = viewModel.lines().count();

        assertTrue(lineCount <= 2700, "DocuPodcastShellViewModel debe quedar bajo el límite transitorio hasta RF-TX2: " + lineCount);
        assertTrue(viewModel.contains("withViewState"), "la deuda de navegación/dirty debe seguir visible hasta extraer WorkspaceNavigationCoordinator");

        String audit = read("docs/productizacion/AUDITORIA_EJECUTABLE_CEREBRO_T61.md");
        assertTrue(audit.contains("DocuPodcastShellViewModel"));
        assertTrue(audit.contains("SourceDocumentRefreshCoordinator"));
        assertTrue(audit.contains("WorkspaceNavigationCoordinator"));
        assertTrue(audit.contains("no aumentar `DocuPodcastShellViewModel` sin registrar deuda"));
        assertTrue(read("docs/productizacion/T107_RAIL_DERECHO_RETRACTIL_REDIMENSIONABLE.md").contains("DocumentMediaWorkflowCoordinator"));
        assertTrue(audit.contains("ReadingComfortCoordinator"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
