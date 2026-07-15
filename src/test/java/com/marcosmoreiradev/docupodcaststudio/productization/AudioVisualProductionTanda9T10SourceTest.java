package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioVisualProductionTanda9T10SourceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void exposesAudioVoiceProjectionThroughApplicationServicesWithoutNewWorkspace() throws IOException {
        String audioServices = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/AudioApplicationServices.java");
        String audioProjection = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/BuildAudioVoiceProductionProjectionUseCase.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String workspacePolicy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceSurfacePolicy.java");

        assertTrue(audioServices.contains("BuildAudioVoiceProductionProjectionUseCase buildAudioVoiceProductionProjection"));
        assertTrue(audioProjection.contains("VoiceReferenceAvailability.MISSING_PROFILE"));
        assertTrue(audioProjection.contains("AudioSourceKind.IMPORTED_AUDIO"));
        assertTrue(audioProjection.contains("FragmentAssetRole.AUDIO_RECORDED"));
        assertTrue(audioProjection.contains("FragmentAssetRole.AMBIENT_AUDIO"));
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_AUDIO_JOBS"));
        assertTrue(workspacePolicy.contains("WorkspaceKind.AUDIO_JOBS"));
        assertTrue(workspacePolicy.contains("LEGACY_INTERNAL_SURFACES"));
    }

    @Test
    void exposesNarrativeVisualProductionOnlyForNarrativeVideo() throws IOException {
        String commandIds = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");

        assertTrue(commandIds.contains("OPEN_NARRATIVE_VISUAL_PRODUCTION"));
        assertTrue(policy.contains("OPEN_NARRATIVE_VISUAL_PRODUCTION -> viewModel.currentProjectModeProperty().get() == ProjectMode.NARRATIVE_VIDEO"));
        assertTrue(ribbon.contains("cmd(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION, true)"));
        assertTrue(shell.contains("NarrativeVisualProductionWorkspaceView"));
        assertTrue(registry.contains("Produccion visual narrativa"));
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(ribbon.contains("OPEN_STORYBOARD"));
    }

    @Test
    void visualProductionIsASeparateProjectionAndServiceNotRootSchema() throws IOException {
        String applicationServices = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ApplicationServices.java");
        String visualServices = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/VisualProductionApplicationServices.java");
        String visualProjection = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/visual/BuildVisualProductionProjectionUseCase.java");
        String project = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/DocuPodcastProject.java");

        assertTrue(applicationServices.contains("VisualProductionApplicationServices visual"));
        assertTrue(visualServices.contains("BuildVisualProductionProjectionUseCase buildVisualProductionProjection"));
        assertTrue(visualProjection.contains("FragmentAssetRole.MAIN_IMAGE"));
        assertTrue(visualProjection.contains("FragmentAssetRole.BRIDGE_TO_NEXT_FRAGMENT"));
        assertTrue(visualProjection.contains("FragmentAssetRole.THEATRE_VISUAL"));
        assertFalse(project.contains("VisualProductionProjection"));
        assertFalse(project.contains("AudioVoiceProductionProjection"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
