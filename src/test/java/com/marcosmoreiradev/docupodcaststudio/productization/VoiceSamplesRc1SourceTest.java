package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOICE-SAMPLES-RC1 + VOICE-WORKSPACE-HF3 + VOICE-TONE-UX-HF1 guardrails. */
final class VoiceSamplesRc1SourceTest {
    @Test
    void voiceSamplesCanOperateFromApplicationLibraryWithoutSavedProject() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/ImportVoiceSampleUseCase.java");
        String repository = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/voice/LocalVoiceSampleFileRepository.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/VoiceSampleWorkflowCoordinator.java");

        assertTrue(viewModel.contains("voiceLibraryProjectFileOrNull"));
        assertTrue(viewModel.contains("DocuPodcastProject.empty(\"Biblioteca de voces\")"));
        assertFalse(viewModel.contains("Guarda el proyecto antes de registrar una muestra de voz."));
        assertFalse(viewModel.contains("Guarda el proyecto antes de grabar una muestra de voz."));
        assertTrue(useCase.contains("effectiveProjectFile"));
        assertTrue(repository.contains("tmp-recordings"));
        assertTrue(workflow.contains("temporaryRecordingDirectory"));
    }

    @Test
    void voiceWorkspaceUsesOperationalRecordingControls() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editorPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/RecordingApplicationServices.java");
        String voices = view + editorPanel + actions;

        assertTrue(voices.contains("voiceProfileSampleEditorModule"));
        assertTrue(voices.contains("Guardar / actualizar voz"));
        assertTrue(voices.contains("Grabar muestra"));
        assertTrue(voices.contains("Detener y guardar"));
        assertTrue(voices.contains("Cancelar grabación"));
        assertTrue(view.contains("voiceSampleActionGroups"));
        assertTrue(viewModel.contains("cancelOwnVoiceRecording"));
        assertTrue(services.contains("CancelAudioRecordingUseCase"));
    }

    @Test
    void documentToneSelectorOnlyShowsRegisteredTonesForSelectedVoice() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");
        assertTrue(panel.contains("registeredDocumentTones"));
        assertTrue(panel.contains("sampleSet.registeredTones()"));
        assertTrue(panel.contains("sampleSet.hasNeutral()"));
        assertTrue(panel.contains("El campo Tono solo mostrará emociones que esa voz ya tenga grabadas"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
