package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T120C applies the final ribbon labels and keeps legacy visual commands out of the ribbon surface. */
final class RibbonFinalApplicationT120CSourceTest {
    @Test
    void finalRibbonAppliesUserIntentLabelsAndNoLegacyVisualWorkspace() throws Exception {
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbarProvider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));

        assertTrue(ribbon.contains("tab(\"inicio\", \"Inicio\""));
        assertTrue(ribbon.contains("tab(\"lectura\", \"Lectura\""));
        assertTrue(ribbon.contains("tab(\"vista\", \"Vista\""));
        assertTrue(ribbon.contains("tab(\"exportar\", \"Exportar\""));
        assertTrue(ribbon.contains("group(\"Vistas principales\""));
        assertTrue(ribbon.contains("AppCommandId.OPEN_DOCUMENT_READER"));
        assertTrue(ribbon.contains("AppCommandId.OPEN_VOICE_LIBRARY"));
        assertFalse(ribbon.contains("cmd(AppCommandId.CREATE_STORYBOARD"));
        assertFalse(ribbon.contains("cmd(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(ribbon.contains("\"Storyboard\""));

        assertTrue(registry.contains("PREPARE_DOCUMENT_READING, \"Preparar lectura\""));
        assertTrue(registry.contains("\"Preparar la lectura del documento antes de generar audio.\""));
        assertFalse(registry.contains("PREPARE_DOCUMENT_READING, \"Preparar audio\""));
        assertFalse(registry.contains("narración interna necesaria"));
        assertTrue(registry.contains("OPEN_VOICE_LIBRARY, \"Biblioteca de voces\""));
        assertFalse(registry.contains("Voces y personajes"));
        assertTrue(registry.contains("CANCEL_AUDIO_JOB, \"Cancelar generación\""));
        assertFalse(registry.contains("CANCEL_AUDIO_JOB, \"Cancelar audio\""));

        String createVisualLine = registry.lines()
                .filter(line -> line.contains("AppCommandId.CREATE_STORYBOARD"))
                .findFirst()
                .orElseThrow();
        assertTrue(createVisualLine.contains("surfaces(AppCommandSurface.TOOLBAR, AppCommandSurface.MENU_BAR)"));
        assertFalse(createVisualLine.contains("AppCommandSurface.RIBBON"));
        String openVisualLine = registry.lines()
                .filter(line -> line.contains("AppCommandId.OPEN_STORYBOARD"))
                .findFirst()
                .orElseThrow();
        assertFalse(openVisualLine.contains("AppCommandSurface.RIBBON"));

        assertTrue(shell.contains("new RibbonView(viewModel, this::dispatchCommand, ribbonStateCoordinator.collapsedProperty())"));
        assertTrue(shell.contains("ribbonStateCoordinator::toggleCollapsed"));
        assertTrue(shell.contains("AppCommandId.OPEN_DOCUMENT_READER"));
        assertTrue(shell.contains("AppCommandId.OPEN_VOICE_LIBRARY"));
        assertTrue(toolbarProvider.contains("Preparar lectura"));
        assertFalse(toolbarProvider.contains("Preparar audio"));
    }
}
