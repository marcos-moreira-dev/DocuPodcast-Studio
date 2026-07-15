package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceGeneratedTestT121V05SourceTest {
    @Test
    void voiceLibraryExposesEditableGeneratedTestWithoutTechnicalEngineNames() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String generatedPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceGeneratedTestPanel.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/VoiceSampleWorkflowCoordinator.java"));
        String css = Files.readString(Path.of("src/main/resources/css/voice-library.css"));

        assertTrue(workspace.contains("TextArea"));
        assertTrue(workspace.contains("Voz de prueba"));
        assertTrue(generatedPanel.contains("VoiceActionStrip.of"));
        assertTrue(workspace.contains("Reproducir voz de prueba generada"));
        assertTrue(workspace.contains("Generando voz..."));
        String narrativeCoordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerCoordinator.java"));
        assertTrue(viewModel.contains("se usar\u00e1 Neutral") || narrativeCoordinator.contains("se usar\u00e1 Neutral"));
        assertTrue(workspace.contains("new Task"));
        assertTrue(workspace.contains("prepareVoiceTestGeneration"));
        assertTrue(workspace.contains("completeVoiceTestGeneration"));
        assertTrue(coordinator.contains("VoiceGeneratedTestRequest"));
        assertTrue(viewModel.contains("prepareVoiceTestGeneration"));
        assertTrue(viewModel.contains("playLastGeneratedVoiceTest"));
        assertTrue(viewModel.contains("Voz de prueba lista"));
        assertTrue(viewModel.contains("Reproduciendo voz de prueba"));
        assertTrue(css.contains("voice-generated-test-text"));
        assertTrue(css.contains("voice-generated-test-status"));
        assertFalse(workspace.contains("Coqui"));
        assertFalse(workspace.contains("XTTS"));
    }

    @Test
    void applicationVoiceServicesExposeGeneratedTestUseCases() throws Exception {
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/VoiceApplicationServices.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/GenerateVoiceTestUseCase.java"));

        assertTrue(services.contains("GenerateVoiceTestUseCase"));
        assertTrue(services.contains("ResolveVoiceToneReferenceUseCase"));
        assertTrue(factory.contains("new GenerateVoiceTestUseCase"));
        assertTrue(useCase.contains("voice-generated-test-v1"));
        assertTrue(useCase.contains("Voz IA avanzada"));
    }
}
