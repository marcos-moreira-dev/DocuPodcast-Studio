package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RibbonBaseT101SourceTest {
    @Test
    void shellUsesTabbedRibbonInsteadOfLegacyMainToolbar() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String module = read("src/main/java/module-info.java");

        assertTrue(shell.contains("new RibbonView(viewModel, this::dispatchCommand, ribbonStateCoordinator.collapsedProperty())"));
        assertTrue(shell.contains("ribbon.visibleProperty().bind(viewModel.projectOpenProperty())"));
        assertTrue(shell.contains("ribbon.managedProperty().bind(viewModel.projectOpenProperty())"));
        assertTrue(shell.contains("RibbonStateCoordinator"));
        assertTrue(shell.contains("presentation.ribbon.RibbonView"));
        assertFalse(shell.contains("new MainToolbarView"), "T101 reemplaza la toolbar legacy como superficie superior principal.");
        assertFalse(shell.contains("presentation.toolbar.MainToolbarView"));

        assertTrue(ribbon.contains("extends VBox"));
        assertTrue(ribbon.contains("ToggleGroup"));
        assertTrue(ribbon.contains("RibbonButton"));
        assertTrue(ribbon.contains("RibbonGroup"));
        assertTrue(ribbon.contains("AppCommandRegistry.official()"));
        assertTrue(ribbon.contains("RibbonDefinitionCatalog.officialTabs(viewModel.currentProjectModeProperty().get())"));
        assertTrue(ribbon.contains("currentProjectModeProperty().addListener"));
        assertTrue(ribbon.contains("dispatchFromRibbon(command.commandId())"));
        assertTrue(ribbon.contains("private void dispatchFromRibbon(AppCommandId commandId)"));
        assertTrue(ribbon.contains("String tabId = selectedTabId"));
        assertTrue(ribbon.contains("selectTab(tabId)"));
        assertTrue(module.contains("exports com.marcosmoreiradev.docupodcaststudio.presentation.ribbon"));
    }

    @Test
    void ribbonDefinesProductTabsAndDoesNotExposeLegacyWorkspaces() throws Exception {
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String css = read("src/main/resources/css/components/ribbon.css");
        String light = read("src/main/resources/css/docupodcast-light.css");

        assertTrue(catalog.contains("tab(\"inicio\", \"Inicio\""));
        assertTrue(catalog.contains("tab(\"lectura\", \"Lectura\""));
        assertFalse(catalog.contains("tab(\"storyboard\""));
        assertFalse(catalog.contains("\"Storyboard\""));
        assertTrue(catalog.contains("tab(\"vista\", \"Vista\""));
        assertTrue(catalog.contains("tab(\"estudio\", \"Estudio\""));
        assertTrue(catalog.contains("tab(\"video-narrativo\", \"Video narrativo\""));
        assertTrue(catalog.contains("tab(\"teatro\", \"Teatro\""));
        assertTrue(catalog.contains("tab(\"exportar\", \"Exportar\""));
        assertFalse(catalog.contains("tab(\"medios\""));
        assertFalse(catalog.contains("tab(\"fragmento\""));

        assertTrue(catalog.contains("AppCommandId.OPEN_SOURCE_DOCUMENT"));
        assertFalse(catalog.contains("AppCommandId.LISTEN_DOCUMENT"));
        assertFalse(catalog.contains("AppCommandId.PLAY_SELECTION"));
        assertTrue(catalog.contains("AppCommandId.OPEN_DOCUMENT_READER"));
        assertTrue(catalog.contains("AppCommandId.OPEN_VOICE_LIBRARY"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_PROJECT_BUNDLE"));
        assertFalse(catalog.contains("AppCommandId.OPEN_EXPORTS_FOLDER"));
        assertFalse(catalog.contains("AppCommandId.TOGGLE_RIGHT_RAIL"), "UX-RIBBON3: el panel visual se controla desde Documento, no desde Ribbon transversal.");
        assertFalse(catalog.contains("group(\"Panel visual\""));
        assertTrue(catalog.contains("group(\"Vistas principales\""));
        assertTrue(catalog.contains("group(\"Produccion visual\""));
        assertFalse(catalog.contains("group(\"Obra\""));
        assertTrue(catalog.contains("group(\"Gestion avanzada de obra\""));
        assertTrue(catalog.contains("group(\"Gestion manual de la obra\""));
        assertTrue(registry.contains("Mostrar módulos estructurales"));
        assertTrue(catalog.contains("group(\"Centro de exportaciones\""));
        assertFalse(catalog.contains("group(\"Salidas creativas\""));
        assertFalse(catalog.contains("AppCommandId.EXPORT_THEATRE_WORK"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(catalog.indexOf("group(\"Soporte\"") < catalog.indexOf("tab(\"lectura\""));

        assertFalse(catalog.contains("OPEN_AUDIO_JOBS"));
        assertFalse(catalog.contains("OPEN_STORYBOARD"));
        assertFalse(catalog.contains("SCRIPT_EDITOR"));
        assertFalse(catalog.contains("AUDIO_JOBS"));
        assertFalse(catalog.contains("Whisper"));
        assertFalse(catalog.contains("STT"));

        assertTrue(registry.contains("AppCommandId.TOGGLE_RIGHT_RAIL"));
        assertTrue(registry.contains("AppCommandSurface.RIGHT_RAIL"));
        assertTrue(css.contains(".ui-ribbon-surface"));
        assertTrue(css.contains(".ui-ribbon-tab"));
        assertTrue(css.contains(".ui-ribbon-content"));
        assertTrue(light.contains("components/ribbon.css"));
    }

    @Test
    void ribbonKeepsPlaybarAsFuturePrimaryOwnerOfListening() throws Exception {
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String commandRegistry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");

        assertTrue(commandRegistry.contains("AppCommandSurface.WORKSPACE_PLAYBAR"));
        assertFalse(catalog.contains("AppCommandId.LISTEN_DOCUMENT"));
        assertFalse(catalog.contains("AppCommandId.PLAY_SELECTION"));
        assertFalse(ribbon.contains("documentPrimaryActionLabelProperty"));
        assertTrue(ribbon.contains("keeping listening controls outside"));
        assertFalse(ribbon.contains("new MainToolbarView"));
    }

    @Test
    void ribbonUsesBreathableDesktopStripWithoutEllipsisRisk() throws Exception {
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonStateCoordinator.java");
        String button = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonButton.java");
        String group = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonGroup.java");
        String css = read("src/main/resources/css/components/ribbon.css");

        assertTrue(ribbon.contains("setPrefHeight(154)"));
        assertTrue(ribbon.contains("setPrefViewportHeight(116)"));
        assertTrue(button.contains("new HBox(8"));
        assertTrue(button.contains("setPrefHeight(72)"));
        assertTrue(button.contains("setWrapText(true)"));
        assertTrue(button.contains("setTextOverrun(OverrunStyle.CLIP)"));
        assertFalse(button.contains("new VBox(4"));
        assertTrue(group.contains("setPrefHeight(110)"));
        assertTrue(css.contains("mas respirable"));
        assertTrue(css.contains("-fx-pref-height: 154px"));
        assertTrue(button.contains("setPrefWidth(232)"));
        assertTrue(ribbon.contains("UI_RIBBON_COLLAPSE_TOGGLE"));
        assertTrue(ribbon.contains("dispatcher.accept(AppCommandId.TOGGLE_RIBBON_COLLAPSED)"));
        assertTrue(ribbon.contains("if (collapsed.get())"));
        assertTrue(ribbon.contains("collapsed.set(false)"));
        assertTrue(ribbon.contains("AppStyles.UI_RIBBON_EXPANDED"));
        assertTrue(ribbon.contains("AppStyles.UI_RIBBON_COLLAPSED"));
        assertTrue(state.contains(".remove(PREF_RIBBON_COLLAPSED)"));
        assertFalse(state.contains("getBoolean(PREF_RIBBON_COLLAPSED"));
        assertFalse(state.contains("putBoolean(PREF_RIBBON_COLLAPSED"));
        assertTrue(css.contains(".ui-ribbon-collapse-toggle"));
        assertTrue(css.contains(".ui-ribbon-surface.ui-ribbon-collapsed"));
        assertTrue(css.contains(".ui-ribbon-surface.ui-ribbon-collapsed .ui-ribbon-content-scroll"));
        assertTrue(css.contains("-fx-pref-height: 34px"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
