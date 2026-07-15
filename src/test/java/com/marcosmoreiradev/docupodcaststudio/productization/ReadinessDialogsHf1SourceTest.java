package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReadinessDialogsHf1SourceTest {
    @Test
    void exportReadinessCommandShowsDialogForBlockedOutputs() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(shell.contains("handleInspectExportReadiness"));
        assertTrue(shell.contains("viewModel.inspectExportReadinessDecision()"));
        assertTrue(vm.contains("Exportación con bloqueos"));
        assertTrue(vm.contains("report.hasBlockedOutput"));
        assertTrue(vm.contains("item.missingRequirements()"));
    }
}
