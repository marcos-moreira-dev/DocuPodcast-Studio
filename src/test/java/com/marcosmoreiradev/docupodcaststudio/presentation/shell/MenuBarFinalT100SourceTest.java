package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T100 guardrail: the MenuBar is the final command catalog for top-level navigation. */
final class MenuBarFinalT100SourceTest {
    @Test
    void menuBarExposesFinalTopLevelCommandsThroughAppCommandIds() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String commandIds = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java"));
        String menuBlock = shell.substring(shell.indexOf("private MenuBar buildMenuBar()"), shell.indexOf("public void handleOpenProjectFolder()"));

        assertTrue(commandIds.contains("OPEN_SOURCE_DOCUMENT_LOCATION"));
        assertTrue(commandIds.contains("OPEN_EXPORTS_FOLDER"));
        assertTrue(commandIds.contains("TOGGLE_RIGHT_RAIL"));

        assertTrue(registry.contains("Abrir ubicación de la fuente"));
        assertTrue(registry.contains("Abrir carpeta de exportaciones"));
        assertTrue(registry.contains("Mostrar u ocultar panel visual"));
        assertTrue(registry.contains("Exportar video"));
        assertFalse(registry.contains("Exportar paquete de video simple"));

        assertTrue(menuBlock.contains("commandItem(AppCommandId.OPEN_SOURCE_DOCUMENT_LOCATION)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.OPEN_EXPORTS_FOLDER)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.TOGGLE_RIGHT_RAIL)"));
        assertTrue(menuBlock.contains("commandItem(AppCommandId.INSPECT_EXPORT_READINESS)"));

        assertTrue(shell.contains(".register(AppCommandId.OPEN_SOURCE_DOCUMENT_LOCATION, this::handleOpenSourceDocumentLocation)"));
        assertTrue(shell.contains(".register(AppCommandId.OPEN_EXPORTS_FOLDER, this::handleOpenExportsFolder)"));
        assertTrue(shell.contains(".register(AppCommandId.TOGGLE_RIGHT_RAIL, viewModel::toggleDocumentRightRail)"));

        assertFalse(shell.contains("setText(\"Exportar paquete de storyboard\")"),
                "El menu no debe reetiquetar video simple como storyboard desde la vista.");
    }

    @Test
    void documentRailCommandControlsTheRealRailState() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(viewModel.contains("documentRightRailVisible"));
        assertTrue(viewModel.contains("toggleDocumentRightRail()"));
        assertFalse(document.contains("rail.expandedProperty().bindBidirectional(viewModel.documentRightRailVisibleProperty())"));
        assertTrue(theatreDock.contains("documentRightRailVisibleProperty().addListener"));
    }
}
