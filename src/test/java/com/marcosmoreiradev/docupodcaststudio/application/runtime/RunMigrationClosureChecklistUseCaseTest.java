package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RunMigrationClosureChecklistUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void reportsPassingGuardrailsFromSourceContracts() throws Exception {
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java",
                "class DocuPodcastShellViewModel {}\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java",
                "hidden(AppCommandId.OPEN_STORYBOARD); hidden(AppCommandId.OPEN_AUDIO_JOBS);\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java",
                "cmd(AppCommandId.OPEN_EXPORT_CENTER);\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/ProjectMode.java",
                "DOCUMENTARY_STUDIO NARRATIVE_VIDEO THEATRE_PRODUCTION\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/fragment/FragmentId.java",
                "record FragmentId(String value) {}\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterCoordinator.java",
                "class ExportCenterCoordinator {}\n");

        ArchitectureGuardrailReport report = new RunMigrationClosureChecklistUseCase().run(tempDir);

        assertTrue(report.passed());
        assertTrue(report.checklistItems().stream().anyMatch(item -> item.contains("Centro de exportaciones")));
    }

    @Test
    void reportsVisibleLegacyWorkspaceAsIssue() throws Exception {
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java",
                "class DocuPodcastShellViewModel {}\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java",
                "command(AppCommandId.OPEN_STORYBOARD);\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java",
                "cmd(AppCommandId.OPEN_STORYBOARD);\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/ProjectMode.java",
                "DOCUMENTARY_STUDIO NARRATIVE_VIDEO THEATRE_PRODUCTION\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/fragment/FragmentId.java",
                "record FragmentId(String value) {}\n");
        write("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterCoordinator.java",
                "class ExportCenterCoordinator {}\n");

        ArchitectureGuardrailReport report = new RunMigrationClosureChecklistUseCase().run(tempDir);

        assertFalse(report.passed());
        assertTrue(report.issues().stream().anyMatch(issue -> issue.contains("OPEN_STORYBOARD")));
    }

    private void write(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
