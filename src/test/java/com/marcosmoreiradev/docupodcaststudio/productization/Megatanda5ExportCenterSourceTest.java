package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Megatanda5ExportCenterSourceTest {
    @Test
    void commandsRibbonAndShellUseCentralExportEntryPoint() throws Exception {
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String ribbonView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");

        assertTrue(ids.contains("OPEN_EXPORT_CENTER"));
        assertTrue(ids.contains("TOGGLE_RIBBON_COLLAPSED"));
        assertTrue(registry.contains("Centro de exportaciones"));
        assertTrue(registry.contains("Minimizar o expandir cinta"));
        assertTrue(policy.contains("case OPEN_EXPORT_CENTER"));
        assertTrue(policy.contains("case EXPORT_SIMPLE_VIDEO_PACKAGE -> viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO"));
        assertTrue(ribbon.contains("cmd(AppCommandId.OPEN_EXPORT_CENTER, true)"));
        assertFalse(ribbon.contains("cmd(AppCommandId.TOGGLE_RIBBON_COLLAPSED, false)"));
        assertTrue(ribbonView.contains("dispatcher.accept(AppCommandId.TOGGLE_RIBBON_COLLAPSED)"));
        assertTrue(ribbonView.contains("UI_RIBBON_COLLAPSE_TOGGLE"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_EXPORT_CENTER, this::handleOpenExportCenter)"));
        assertTrue(shell.contains("ExportCenterCoordinator"));
        assertTrue(shell.contains("executeExportCenterTarget"));
    }

    @Test
    void exportMenuKeepsSupportPackageOutAndRoutesCreativeActionsThroughCenter() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String exportMenu = shell.substring(shell.indexOf("Menu exportar = new Menu(\"Exportar\")"), shell.indexOf("Menu configuracion = new Menu"));
        String helpMenu = shell.substring(shell.indexOf("Menu ayuda = new Menu(\"Ayuda\")"), shell.indexOf("menuBar.getMenus().addAll"));

        assertTrue(exportMenu.contains("AppCommandId.OPEN_EXPORT_CENTER"));
        assertTrue(exportMenu.contains("AppCommandId.EXPORT_PODCAST_WAV"));
        assertTrue(exportMenu.contains("AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertTrue(exportMenu.contains("AppCommandId.EXPORT_THEATRE_WORK"));
        assertFalse(exportMenu.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertFalse(exportMenu.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"));
        assertTrue(helpMenu.contains("Soporte avanzado"));
        assertTrue(helpMenu.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_PODCAST_WAV, () -> handleOpenExportCenter(AppCommandId.EXPORT_PODCAST_WAV))"));
        assertTrue(shell.contains("case EXPORT_PODCAST_WAV -> handleExportPodcastWav(selection.audioFormat())"));
    }

    @Test
    void viewModelBudgetAndStoryboardWorkspaceGuardrailsHold() throws Exception {
        Path viewModelPath = Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");

        assertTrue(Files.readAllLines(viewModelPath).size() <= 2600);
        assertFalse(shell.contains("register(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(ribbon.contains("cmd(AppCommandId.OPEN_STORYBOARD"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
