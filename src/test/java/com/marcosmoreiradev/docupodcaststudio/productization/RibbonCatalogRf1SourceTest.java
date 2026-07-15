package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RibbonCatalogRf1SourceTest {
    @Test
    void ribbonViewRendersCatalogInsteadOfOwningProductStructure() throws Exception {
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));

        assertTrue(ribbon.contains("RibbonDefinitionCatalog.officialTabs(viewModel.currentProjectModeProperty().get())"));
        assertTrue(catalog.contains("officialTabs(ProjectMode mode)"));
        assertTrue(catalog.contains("tab(\"inicio\", \"Inicio\""));
        assertTrue(catalog.contains("tab(\"estudio\", \"Estudio\""));
        assertTrue(catalog.contains("tab(\"video-narrativo\", \"Video narrativo\""));
        assertTrue(catalog.contains("tab(\"teatro\", \"Teatro\""));
        assertTrue(catalog.contains("group(\"Vistas principales\""));
        assertTrue(catalog.contains("AppCommandId.OPEN_DOCUMENT_READER"));
        assertTrue(catalog.contains("AppCommandId.OPEN_VOICE_LIBRARY"));
        assertFalse(catalog.contains("OPEN_STORYBOARD"));
        assertFalse(catalog.contains("TOGGLE_RIGHT_RAIL"));
    }
}
