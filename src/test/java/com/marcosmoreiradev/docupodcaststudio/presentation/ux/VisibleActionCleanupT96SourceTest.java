package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T96 guardrail: visible UI surfaces must not expose placeholder actions. */
final class VisibleActionCleanupT96SourceTest {
    @Test
    void menuAndToolbarDoNotWireVisibleActionsToPlaceholders() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String provider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));

        assertFalse(shell.contains("setOnAction(event -> viewModel.showPlaceholder"),
                "No visible menu item should trigger a placeholder workspace.");
        assertFalse(toolbar.contains("showPlaceholder"),
                "Toolbar buttons must not be placeholder shortcuts.");
        assertFalse(provider.contains("Perfil de lectura"),
                "Reading profile must stay hidden until the flow is implemented as a real command.");
    }

    @Test
    void menuHidesUnimplementedReaderUtilitiesUntilTheyAreReal() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        int menuStart = shell.indexOf("private MenuBar buildMenuBar()");
        int menuEnd = shell.indexOf("public void handleOpenProjectFolder()");
        String menuBlock = shell.substring(menuStart, menuEnd);

        assertFalse(menuBlock.contains("Buscar en documento…"));
        assertFalse(menuBlock.contains("Ir a página / fragmento…"));
        assertFalse(menuBlock.contains("Mostrar / ocultar miniaturas"));
        assertFalse(menuBlock.contains("Mostrar / ocultar panel lateral"));
        assertFalse(menuBlock.contains("Modo lectura limpia"));
        assertFalse(menuBlock.contains("Ver información del documento fuente"));
        assertFalse(menuBlock.contains("Diagnóstico de importación…"));
        assertTrue(menuBlock.contains("AppCommandId.OPEN_SOURCE_DOCUMENT"));
        assertTrue(menuBlock.contains("AppCommandId.REFRESH_SOURCE_DOCUMENT"));
    }
}
