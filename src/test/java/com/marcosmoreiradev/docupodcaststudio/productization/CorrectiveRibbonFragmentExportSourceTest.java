package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CorrectiveRibbonFragmentExportSourceTest {
    @Test
    void ribbonUsesSlimTabStripCollapseControlAndRemovesQuickListeningButtons() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String css = read("src/main/resources/css/components/ribbon.css");

        assertFalse(catalog.contains("Escucha rapida"));
        assertFalse(catalog.contains("LISTEN_DOCUMENT"));
        assertFalse(catalog.contains("PLAY_SELECTION"));
        assertFalse(catalog.contains("cmd(AppCommandId.TOGGLE_RIBBON_COLLAPSED, false)"));
        assertTrue(ribbon.contains("UI_RIBBON_COLLAPSE_TOGGLE"));
        assertTrue(ribbon.contains("dispatcher.accept(AppCommandId.TOGGLE_RIBBON_COLLAPSED)"));
        assertTrue(ribbon.contains("Maximizar cinta"));
        assertTrue(ribbon.contains("Minimizar cinta"));
        assertTrue(css.contains(".ui-ribbon-collapse-toggle"));
    }

    @Test
    void fragmentPanelCheckboxMapsToRealReadingProfileAndInvalidatesAudio() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(panel.contains("new CheckBox(\"Leer cuadros de texto y tablas\")"));
        assertTrue(panel.contains("viewModel.setReadTablesAndTextBoxesForNarration(selectedValue)"));
        assertTrue(panel.contains("viewModel.activeReadingProfileProperty().addListener"));
        assertTrue(viewModel.contains("TableNarrationPolicy.READ_STRUCTURED"));
        assertTrue(viewModel.contains("TableNarrationPolicy.IGNORE_TABLES"));
        assertTrue(viewModel.contains("invalidatePersistedAudioForNarrationChange()"));
        assertTrue(viewModel.contains("Reconstruye fragmentos de audio"));
    }

    @Test
    void exportCenterProvidesAudioFormatAndDocumentTextVideoOptions() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterDialog.java");
        String selection = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterSelection.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String options = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/documentstudy/DocumentTextVideoOptions.java");
        String plan = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/documentstudy/BuildDocumentStudyTextVideoPlanUseCase.java");

        assertTrue(dialog.contains("ComboBox<AudioExportFormat>"));
        assertTrue(dialog.contains("AudioExportFormat.values()"));
        assertTrue(dialog.contains("ColorPicker"));
        assertTrue(dialog.contains("Font.getFamilies()"));
        assertTrue(dialog.contains("Spinner<Integer>"));
        assertTrue(dialog.contains("DocumentTextVideoBackgroundMode.IMAGE"));
        assertTrue(selection.contains("AudioExportFormat audioFormat"));
        assertTrue(selection.contains("DocumentTextVideoOptions documentTextVideoOptions"));
        assertTrue(shell.contains("selection.audioFormat()"));
        assertTrue(shell.contains("selection.documentTextVideoOptions()"));
        assertTrue(shell.contains("chooseExportPodcastWavTarget(AudioExportFormat requestedFormat)"));
        assertTrue(options.contains("DocumentTextVideoBackgroundMode backgroundMode"));
        assertTrue(options.contains("String backgroundImagePath"));
        assertTrue(options.contains("String fontFamily"));
        assertTrue(plan.contains("String unitPrefix = segmentId + \"-\""));
        assertTrue(plan.contains("clip.segmentId().startsWith(unitPrefix)"));
    }

    @Test
    void theatreRibbonKeepsWorkCommandsAndMovesFinalExportsToExportTab() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String modePolicy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectModePolicy.java");

        assertTrue(catalog.contains("group(\"Gestion manual de la obra\""));
        assertTrue(catalog.contains("group(\"Gramatica\""));
        assertFalse(catalog.contains("group(\"Obra\""));
        assertTrue(catalog.contains("group(\"Gestion avanzada de obra\""));
        assertFalse(catalog.contains("group(\"Salidas creativas\""));
        assertTrue(catalog.contains("group(\"Centro de exportaciones\""));
        assertTrue(catalog.contains("cmd(AppCommandId.OPEN_EXPORT_CENTER, true)"));
        assertFalse(catalog.contains("cmd(AppCommandId.EXPORT_THEATRE_WORK"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(modePolicy.contains("hasTheatreData(project.theatre())"));
        assertTrue(modePolicy.contains("ProjectMode.THEATRE_PRODUCTION"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
