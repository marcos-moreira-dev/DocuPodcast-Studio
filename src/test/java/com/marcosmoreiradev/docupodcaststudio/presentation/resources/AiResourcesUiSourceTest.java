package com.marcosmoreiradev.docupodcaststudio.presentation.resources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AiResourcesUiSourceTest {
    @Test
    void aiResourcesStayInHelpOrAdvancedSurfacesWithoutToolbarButton() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));

        assertTrue(shell.contains("Menu ayuda = new Menu(\"Ayuda\")"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_AI_RESOURCES, this::handleExportAiResources)"));
        assertTrue(registry.contains("AppCommandId.EXPORT_AI_RESOURCES"));
        assertTrue(registry.contains("Exportar recursos IA"));
        assertTrue(toolbar.contains("workspace-toolbar-area"));
        assertFalse(toolbar.contains("toolbarButton(\"Recursos IA\")"),
                "La exportación/ayuda de recursos IA no debe reintroducir un botón global en la toolbar.");
        assertFalse(toolbar.contains("EXPORT_AI_RESOURCES"),
                "Recursos IA no debe competir con la lectura principal desde la toolbar.");
    }
}
