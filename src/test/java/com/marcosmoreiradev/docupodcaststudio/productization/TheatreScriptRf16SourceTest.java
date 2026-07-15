package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreScriptRf16SourceTest {
    @Test
    void documentReaderIsCleanAndTheatreScriptOwnsTheRightDock() throws Exception {
        String document = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String theatreDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");

        assertTrue(document.contains("DocumentWorkspaceMode.READING"));
        assertTrue(document.contains("DocumentWorkspaceMode.THEATRE_SCRIPT"));
        assertTrue(document.contains("buildTheatreSideDock()"));
        assertTrue(document.contains("SplitPane.setResizableWithParent(theatreSideDock, Boolean.FALSE)"));
        assertTrue(document.contains("splitPane.setDividerPositions(0.19, 0.68)"));
        assertFalse(document.contains("SideDockModuleId.DOCUMENT_IMAGE"));
        assertTrue(theatreDock.contains("THEATRE_FRAGMENT_IMAGES"));
        assertTrue(theatreDock.contains("THEATRE_CHARACTERS"));
        assertTrue(theatreDock.contains("THEATRE_TEXTUAL_MAP"));
        assertTrue(theatreDock.contains("THEATRE_SPATIAL_MAP"));
        assertTrue(theatreDock.contains("THEATRE_ACTIONS"));
        assertTrue(theatreDock.contains("THEATRE_OBJECTS"));
        assertTrue(theatreDock.contains("DocumentImageContextPanel"));
        assertTrue(theatreDock.contains("DocumentMediaRailView"));
        assertTrue(theatreDock.contains("CollapsibleModuleSplitPane"));
        assertTrue(theatreDock.contains("\"Acciones visuales\""));
        assertTrue(theatreDock.contains("theatre-fragment-images-split"));
        assertTrue(theatreDock.contains("COLLAPSED_WIDTH"));
        assertTrue(theatreDock.contains("COMPACT_WIDTH"));
        assertTrue(theatreDock.contains("EXPANDED_WIDTH"));
        assertTrue(theatreDock.contains("setCompactContent"));
        assertTrue(theatreDock.contains("activeModuleIdProperty().addListener"));
        assertTrue(theatreDock.contains("split, split::showPrimary"));
        assertTrue(theatreDock.contains("TheatreAudioTrackWorkspace"));
        assertTrue(theatreDock.contains("applyDockWidth"));
        assertTrue(theatreDock.contains("WorkspaceSideDock.RailPlacement.RIGHT"));
        assertTrue(theatreDock.contains("dock.expandedProperty().addListener"));
    }

    @Test
    void theatreRibbonCommandsOpenScriptAndRoutesExportsThroughCenter() throws Exception {
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(ribbon.contains("tab(\"teatro\", \"Teatro\""));
        assertTrue(ribbon.contains("OPEN_THEATRE_SCRIPT"));
        assertFalse(ribbon.contains("EXPORT_THEATRE_WORK"));
        assertTrue(ribbon.contains("OPEN_EXPORT_CENTER"));
        assertTrue(registry.contains("OPEN_THEATRE_SCRIPT"));
        assertTrue(registry.contains("EXPORT_THEATRE_WORK"));
        assertTrue(shell.contains("showTheatreScriptWorkspace"));
        assertTrue(shell.contains("handleExportTheatreWork"));
        assertTrue(viewModel.contains("WorkspaceKind.THEATRE_SCRIPT"));
    }

    @Test
    void theatreExportDialogContainsVisualWorkOptions() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreWorkExportOptionsDialog.java");
        String options = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreWorkExportOptions.java");

        assertTrue(dialog.contains("Usar imagenes de fragmentos"));
        assertTrue(dialog.contains("Mostrar texto"));
        assertTrue(dialog.contains("Tipografia"));
        assertTrue(dialog.contains("Efecto del texto"));
        assertTrue(dialog.contains("Mapa espacial lateral"));
        assertTrue(dialog.contains("Mostrar personajes"));
        assertTrue(dialog.contains("Mostrar desplazamientos"));
        assertTrue(options.contains("useFragmentImages"));
        assertTrue(options.contains("showSpatialMap"));
        assertTrue(options.contains("showDisplacements"));
    }

    @Test
    void intervencionesTreatAsSequenceAndRolesAsVoiceAliases() throws Exception {
        String layer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/theatre/TheatreProjectLayer.java");
        String placeholder = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatrePlaceholderPanel.java");

        assertTrue(layer.contains("Intervencion"));
        assertTrue(layer.contains("VoiceRoleAlias"));
        assertTrue(layer.contains("ofSequence"));
        assertTrue(layer.contains("\"INTERVENCION-\" + sequenceIndex"));
        assertTrue(layer.contains("El villano"));
        assertTrue(layer.contains("voiceProfileId"));
        assertTrue(placeholder.contains("Intervencion 1, Intervencion 2"));
        assertTrue(placeholder.contains("fragmentos"));
        assertTrue(placeholder.contains("No representan personajes"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
