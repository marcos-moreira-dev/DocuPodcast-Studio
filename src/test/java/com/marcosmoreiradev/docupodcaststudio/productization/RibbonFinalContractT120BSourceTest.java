package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T120B freezes the product contract for the ribbon before the final visual pass. */
final class RibbonFinalContractT120BSourceTest {
    @Test
    void ribbonIsOrganizedByUserIntentAndNotLegacyModules() throws Exception {
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));
        String commands = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(ribbon.contains("tab(\"inicio\", \"Inicio\""));
        assertTrue(ribbon.contains("tab(\"lectura\", \"Lectura\""));
        assertTrue(ribbon.contains("tab(\"vista\", \"Vista\""));
        assertTrue(ribbon.contains("tab(\"estudio\", \"Estudio\""));
        assertTrue(ribbon.contains("tab(\"video-narrativo\", \"Video narrativo\""));
        assertTrue(ribbon.contains("tab(\"teatro\", \"Teatro\""));
        assertTrue(ribbon.contains("tab(\"exportar\", \"Exportar\""));
        assertFalse(ribbon.contains("tab(\"storyboard\""));
        assertFalse(ribbon.contains("\"Storyboard\""));

        assertTrue(ribbon.contains("group(\"Vistas principales\""));
        assertTrue(ribbon.contains("AppCommandId.OPEN_DOCUMENT_READER"));
        assertTrue(ribbon.contains("AppCommandId.OPEN_VOICE_LIBRARY"));
        assertFalse(ribbon.contains("group(\"Panel visual\""));
        assertFalse(ribbon.contains("AppCommandId.TOGGLE_RIGHT_RAIL"));
        assertTrue(ribbon.contains("group(\"Produccion visual\""));
        assertFalse(ribbon.contains("LISTEN_DOCUMENT"));
        assertFalse(ribbon.contains("PLAY_SELECTION"));
        assertTrue(shell.contains("Menu videoNarrativo = new Menu(\"Video narrativo\")"));
        assertTrue(shell.contains("bindModeMenu(videoNarrativo, ProjectMode.NARRATIVE_VIDEO)"));
        assertTrue(commands.contains("OPEN_DOCUMENT_READER"));
        assertTrue(registry.contains("Volver al lector principal del documento"));
        assertTrue(shell.contains("AppCommandId.OPEN_DOCUMENT_READER"));
    }
}
