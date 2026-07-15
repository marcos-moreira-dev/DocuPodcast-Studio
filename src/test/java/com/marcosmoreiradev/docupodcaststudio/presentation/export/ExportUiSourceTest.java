package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportUiSourceTest {
    @Test
    void shellExposesUserFacingExportCommandsThroughCommandCatalogWithoutDiagnosticNoise() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));

        assertTrue(shell.contains("Menu exportar = new Menu(\"Exportar\")"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_EXPORT_CENTER)"));
        assertTrue(shell.contains("commandItem(AppCommandId.EXPORT_PODCAST_WAV)"));
        assertTrue(shell.contains("commandItem(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE)"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_PROJECT_BUNDLE, this::handleExportProjectBundle)"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_EXPORT_CENTER, this::handleOpenExportCenter)"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_PODCAST_WAV, () -> handleOpenExportCenter(AppCommandId.EXPORT_PODCAST_WAV))"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE, () -> handleOpenExportCenter(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE))"));
        assertTrue(shell.contains("case EXPORT_PODCAST_WAV -> handleExportPodcastWav(selection.audioFormat())"));
        assertTrue(shell.contains("case EXPORT_SIMPLE_VIDEO_PACKAGE -> handleExportSimpleVideo(selection.videoOptions())"));
        assertTrue(shell.contains("ProjectExportEligibilityPolicy"));
        assertTrue(shell.contains("exportEligibilityPolicy.evaluate(viewModel.currentDocumentProperty().get())"));
        assertTrue(shell.contains("UserVisibleDecision.warning(eligibility.title(), eligibility.message())"));
        assertTrue(registry.contains("Exportar paquete de soporte"));
        assertTrue(registry.contains("Centro de exportaciones"));
        assertTrue(registry.contains("Exportar audio"));
        assertTrue(registry.contains("Exportar video"));
        assertFalse(registry.contains("Exportar paquete de video simple"));
        assertTrue(shell.contains("commandItem(AppCommandId.OPEN_EXPORTS_FOLDER)"));
        String exportMenuBlock = shell.substring(shell.indexOf("Menu exportar = new Menu(\"Exportar\")"), shell.indexOf("Menu configuracion = new Menu(\"Configuración\")"));
        String helpMenuBlock = shell.substring(shell.indexOf("Menu ayuda = new Menu(\"Ayuda\")"), shell.indexOf("menuBar.getMenus().addAll"));
        assertFalse(exportMenuBlock.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"),
                "El paquete técnico de soporte no debe estar en Exportar.");
        assertFalse(exportMenuBlock.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"),
                "El reporte técnico no debe estar en Exportar.");
        assertTrue(helpMenuBlock.contains("Soporte avanzado"));
        assertTrue(helpMenuBlock.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertTrue(helpMenuBlock.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"));
        assertFalse(shell.contains("new MenuItem(\"Exportar narración Markdown"));
        assertFalse(shell.contains("new MenuItem(\"Exportar guion Markdown"));
        assertFalse(toolbar.contains("shellView.dispatchCommand(AppCommandId.EXPORT_PROJECT_BUNDLE)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.EXPORT_PODCAST_WAV)"));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertTrue(ribbon.contains("AppCommandId.OPEN_EXPORT_CENTER"));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_PODCAST_WAV"));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertFalse(ribbon.contains("AppCommandId.OPEN_EXPORTS_FOLDER"));
    }
}
