package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceRecordingWorkflowSourceTest {
    @Test
    void voiceLibraryExposesStartStopRecordingAndRegistersSample() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String actions = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String voices = workspace + actions;

        assertTrue(voices.contains("Grabar muestra"));
        assertTrue(voices.contains("Detener y guardar"));
        assertTrue(actions.contains("voiceRecordingRunningProperty"));
        assertTrue(viewModel.contains("startOwnVoiceRecording"));
        assertTrue(viewModel.contains("stopOwnVoiceRecording"));
        assertTrue(viewModel.contains("cancelOwnVoiceRecording"));
        assertTrue(viewModel.contains("importVoiceSampleInternal(recorded"));
        assertTrue(viewModel.contains("recordedInJava") || viewModel.contains("VoiceSampleOrigin.RECORDED_IN_APP"));
    }

    @Test
    void recordingUseCasesAreWiredThroughApplicationServices() throws Exception {
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/RecordingApplicationServices.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));
        String infrastructure = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));

        assertTrue(services.contains("StartAudioRecordingUseCase"));
        assertTrue(services.contains("StopAudioRecordingUseCase"));
        assertTrue(services.contains("CancelAudioRecordingUseCase"));
        assertTrue(factory.contains("new StartAudioRecordingUseCase"));
        assertTrue(factory.contains("new StopAudioRecordingUseCase"));
        assertTrue(factory.contains("new CancelAudioRecordingUseCase"));
        assertTrue(infrastructure.contains("new JavaSoundAudioRecordingGateway()"));
    }
}
