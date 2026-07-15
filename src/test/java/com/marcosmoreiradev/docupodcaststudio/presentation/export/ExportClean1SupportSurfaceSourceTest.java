package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportClean1SupportSurfaceSourceTest {
    @Test
    void technicalSupportExportsStayOutOfCommonExportToolbarAndRibbonSurfaces() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));

        String exportMenu = shell.substring(shell.indexOf("Menu exportar = new Menu(\"Exportar\")"), shell.indexOf("Menu configuracion = new Menu(\"Configuración\")"));
        String helpMenu = shell.substring(shell.indexOf("Menu ayuda = new Menu(\"Ayuda\")"), shell.indexOf("menuBar.getMenus().addAll"));

        assertTrue(exportMenu.contains("AppCommandId.EXPORT_PODCAST_WAV"));
        assertTrue(exportMenu.contains("AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertFalse(exportMenu.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertFalse(exportMenu.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"));

        assertTrue(helpMenu.contains("Soporte avanzado"));
        assertTrue(helpMenu.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertTrue(helpMenu.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"));
        assertTrue(helpMenu.contains("AppCommandId.INSPECT_PROJECT_INTEGRITY"));

        assertTrue(toolbar.contains("AppCommandId.EXPORT_PODCAST_WAV"));
        assertFalse(toolbar.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertFalse(toolbar.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"));

        assertTrue(ribbon.contains("AppCommandId.OPEN_EXPORT_CENTER"));
        assertTrue(ribbon.contains("group(\"Centro de exportaciones\""));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_PODCAST_WAV"));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertFalse(ribbon.contains("AppCommandId.OPEN_EXPORTS_FOLDER"));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertFalse(ribbon.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"));

        assertTrue(registry.contains("Exportar paquete de soporte"));
        assertTrue(registry.contains("Exportar reporte de soporte"));
        String bundleLine = registry.lines()
                .filter(line -> line.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"))
                .findFirst()
                .orElse("");
        String diagnosticLine = registry.lines()
                .filter(line -> line.contains("AppCommandId.EXPORT_DIAGNOSTIC_REPORT"))
                .findFirst()
                .orElse("");
        assertFalse(bundleLine.contains("AppCommandSurface.RIBBON"));
        assertFalse(bundleLine.contains("AppCommandSurface.TOOLBAR"));
        assertFalse(diagnosticLine.contains("AppCommandSurface.RIBBON"));
        assertFalse(diagnosticLine.contains("AppCommandSurface.TOOLBAR"));
    }
}
