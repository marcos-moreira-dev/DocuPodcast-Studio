package com.marcosmoreiradev.docupodcaststudio.productization;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandDescriptor;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandRegistry;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandSurface;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.ribbon.RibbonDefinitionCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.ribbon.RibbonTabDefinition;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tanda 8: the ribbon is a product surface over the official command catalog. */
final class RibbonCommandContractTanda8Test {
    @Test
    void everyRibbonCommandIsRegisteredVisibleImplementedAndHandled() throws Exception {
        AppCommandRegistry registry = AppCommandRegistry.official();
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        Set<AppCommandId> ids = ribbonCommandIds();

        for (AppCommandId id : ids) {
            AppCommandDescriptor descriptor = registry.descriptor(id);
            assertTrue(descriptor.visibleByDefault(), id + " debe ser visible por defecto si vive en ribbon.");
            assertTrue(descriptor.implemented(), id + " debe estar implementado si vive en ribbon.");
            assertTrue(descriptor.allowedOn(AppCommandSurface.RIBBON), id + " debe declarar superficie ribbon.");
            assertTrue(shell.contains(".register(AppCommandId." + id.name()), id + " debe tener handler en shell.");
        }
    }

    @Test
    void registryRibbonSurfaceMatchesAllModeCatalogsAndTabsAreNotEmpty() {
        AppCommandRegistry registry = AppCommandRegistry.official();
        Set<AppCommandId> catalogIds = ribbonCommandIds();
        for (ProjectMode mode : ProjectMode.officialModes()) {
            for (RibbonTabDefinition tab : RibbonDefinitionCatalog.officialTabs(mode)) {
                assertFalse(tab.groups().isEmpty(), "Tab sin grupos en " + mode + ": " + tab.id());
                assertTrue(tab.groups().stream().anyMatch(group -> !group.commands().isEmpty()),
                        "Tab sin comandos en " + mode + ": " + tab.id());
                for (var group : tab.groups()) {
                    LinkedHashSet<AppCommandId> seen = new LinkedHashSet<>();
                    for (var command : group.commands()) {
                        assertTrue(seen.add(command.commandId()),
                                "Comando duplicado dentro del grupo " + group.title() + ": " + command.commandId());
                    }
                }
            }
        }

        Set<AppCommandId> registryRibbonIds = registry.visibleOn(AppCommandSurface.RIBBON).stream()
                .map(AppCommandDescriptor::id)
                .filter(id -> id != AppCommandId.TOGGLE_RIBBON_COLLAPSED)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AppCommandId.class)));

        assertEquals(catalogIds, registryRibbonIds);
    }

    @Test
    void projectModeTabsAreContextualAndLecturaHasNoNarrativeOnlyCommands() {
        assertTrue(tabIds(ProjectMode.DOCUMENTARY_STUDIO).contains("estudio"));
        assertFalse(tabIds(ProjectMode.DOCUMENTARY_STUDIO).contains("video-narrativo"));
        assertFalse(tabIds(ProjectMode.DOCUMENTARY_STUDIO).contains("teatro"));

        assertTrue(tabIds(ProjectMode.NARRATIVE_VIDEO).contains("video-narrativo"));
        assertFalse(tabIds(ProjectMode.NARRATIVE_VIDEO).contains("teatro"));

        assertTrue(tabIds(ProjectMode.THEATRE_PRODUCTION).contains("teatro"));
        assertFalse(tabIds(ProjectMode.THEATRE_PRODUCTION).contains("video-narrativo"));

        RibbonTabDefinition lectura = RibbonDefinitionCatalog.officialTabs(ProjectMode.NARRATIVE_VIDEO).stream()
                .filter(tab -> tab.id().equals("lectura"))
                .findFirst()
                .orElseThrow();
        Set<AppCommandId> lecturaCommands = lectura.groups().stream()
                .flatMap(group -> group.commands().stream())
                .map(command -> command.commandId())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AppCommandId.class)));
        assertFalse(lecturaCommands.contains(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION));
        assertFalse(lecturaCommands.contains(AppCommandId.IMPORT_NARRATIVE_VIDEO_GRAMMAR));
        assertFalse(lecturaCommands.contains(AppCommandId.EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE));
    }

    @Test
    void questionableTechnicalAndContextualCommandsStayOutOfRibbon() {
        Set<AppCommandId> ids = ribbonCommandIds();

        assertFalse(ids.contains(AppCommandId.OPEN_STORYBOARD));
        assertFalse(ids.contains(AppCommandId.OPEN_AUDIO_JOBS));
        assertFalse(ids.contains(AppCommandId.EXPORT_PROJECT_BUNDLE));
        assertFalse(ids.contains(AppCommandId.EXPORT_DIAGNOSTIC_REPORT));
        assertFalse(ids.contains(AppCommandId.EXPORT_PODCAST_WAV));
        assertFalse(ids.contains(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO));
        assertFalse(ids.contains(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE));
        assertFalse(ids.contains(AppCommandId.EXPORT_THEATRE_WORK));
        assertFalse(ids.contains(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW));
        assertFalse(ids.contains(AppCommandId.EXPORT_THEATRE_PORTION));
        assertFalse(ids.contains(AppCommandId.OPEN_EXPORTS_FOLDER));
        assertFalse(ids.contains(AppCommandId.INSPECT_EXPORT_READINESS));
        assertFalse(ids.contains(AppCommandId.INSPECT_PROJECT_INTEGRITY));
        assertFalse(ids.contains(AppCommandId.EXPORT_AI_RESOURCES));
        assertFalse(ids.contains(AppCommandId.CLEAR_SELECTION));
        assertFalse(ids.contains(AppCommandId.LISTEN_DOCUMENT));
        assertFalse(ids.contains(AppCommandId.PLAY_SELECTION));
        assertFalse(ids.contains(AppCommandId.ASSIGN_AI_VOICE_TO_SELECTION));
        assertFalse(ids.contains(AppCommandId.ASSIGN_HUMAN_RECORDING_TO_SELECTION));
        assertFalse(ids.contains(AppCommandId.IMPORT_VOICE_SAMPLE));
        assertFalse(ids.contains(AppCommandId.IMPORT_AUDIO_FOR_SELECTION));
        assertFalse(ids.contains(AppCommandId.EXTRACT_VIDEO_AUDIO_FOR_SELECTION));
        assertFalse(ids.contains(AppCommandId.IMPORT_IMAGE_FOR_SELECTION));
        assertFalse(ids.contains(AppCommandId.IMPORT_BRIDGE_IMAGE_FOR_SELECTION));
        assertFalse(ids.contains(AppCommandId.ASSOCIATE_IMAGE_TO_SELECTION));
        assertFalse(ids.contains(AppCommandId.OPEN_WORD_GUIDE));
    }

    @Test
    void exportTabKeepsOneCenterEntryAndRoutesDirectActionsThroughShell() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");

        assertFalse(catalog.contains("group(\"Obra\""));
        assertFalse(catalog.contains("group(\"Salidas creativas\""));
        assertTrue(catalog.contains("group(\"Centro de exportaciones\""));
        assertTrue(catalog.contains("cmd(AppCommandId.OPEN_EXPORT_CENTER, true)"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_THEATRE_WORK"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW"));
        assertFalse(catalog.contains("AppCommandId.EXPORT_THEATRE_PORTION"));
        assertFalse(catalog.contains("AppCommandId.OPEN_EXPORTS_FOLDER"));

        assertTrue(policy.contains("EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO -> viewModel.currentProjectModeProperty().get() == ProjectMode.DOCUMENTARY_STUDIO"));
        assertTrue(policy.contains("EXPORT_SIMPLE_VIDEO_PACKAGE -> viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO"));
        assertTrue(policy.contains("EXPORT_THEATRE_WORK, EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(policy.contains("capabilities.theatreProduction()"));

        assertTrue(shell.contains(".register(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, () -> handleOpenExportCenter(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO))"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE, () -> handleOpenExportCenter(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE))"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_THEATRE_WORK, () -> handleOpenExportCenter(AppCommandId.EXPORT_THEATRE_WORK))"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW, () -> handleOpenExportCenter(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW))"));
        assertTrue(shell.contains(".register(AppCommandId.EXPORT_THEATRE_PORTION, () -> handleOpenExportCenter(AppCommandId.EXPORT_THEATRE_PORTION))"));
    }

    @Test
    void ribbonTooltipUsesSharedUnavailableReasonPolicy() throws Exception {
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String button = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonButton.java");

        assertTrue(policy.contains("unavailableReasonBinding(AppCommandId commandId, DocuPodcastShellViewModel viewModel)"));
        assertTrue(ribbon.contains("commandAvailabilityPolicy.unavailableReasonBinding(command.commandId(), viewModel)"));
        assertTrue(ribbon.contains("descriptor.description() + \"\\n\" + unavailableReason.get()"));
        assertTrue(button.contains("ObservableValue<String> tooltip"));
        assertTrue(button.contains("tooltipNode.textProperty().bind"));
    }

    @Test
    void documentationRecordsTanda8DecisionMatrix() throws Exception {
        String doc = read("DOCUMENTACION_ACTUAL/TANDA_08_RIBBON_COMANDOS/RIBBON_COMMAND_CONTRACT.md");

        assertTrue(doc.contains("Tanda 8"));
        assertTrue(doc.contains("EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO"));
        assertTrue(doc.contains("IMPORT_BRIDGE_IMAGE_FOR_SELECTION"));
        assertTrue(doc.contains("ocultar"));
        assertTrue(doc.contains("mvn -q test"));
    }

    private static Set<AppCommandId> ribbonCommandIds() {
        return ribbonCommandList().stream()
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AppCommandId.class)));
    }

    private static List<AppCommandId> ribbonCommandList() {
        return ProjectMode.officialModes().stream()
                .flatMap(mode -> RibbonDefinitionCatalog.officialTabs(mode).stream())
                .flatMap((RibbonTabDefinition tab) -> tab.groups().stream())
                .flatMap(group -> group.commands().stream())
                .map(command -> command.commandId())
                .toList();
    }

    private static Set<String> tabIds(ProjectMode mode) {
        return RibbonDefinitionCatalog.officialTabs(mode).stream()
                .map(RibbonTabDefinition::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
