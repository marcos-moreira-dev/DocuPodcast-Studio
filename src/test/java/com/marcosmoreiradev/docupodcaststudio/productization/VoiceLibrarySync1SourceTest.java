package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOICE-LIBRARY-SYNC1: voice samples live in the app library and Document sees only registered tones. */
final class VoiceLibrarySync1SourceTest {
    @Test
    void productWiringStoresVoiceSamplesInApplicationLevelLibrary() throws Exception {
        String runtime = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ApplicationRuntimeLayout.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));
        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/voice/LocalVoiceSampleFileRepository.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/ImportVoiceSampleUseCase.java"));

        assertTrue(runtime.contains("voiceLibraryRoot"));
        assertTrue(runtime.contains("voiceSamplesRoot"));
        assertTrue(factory.contains("new LocalVoiceSampleFileRepository(runtimeLayout.applicationRoot())"));
        assertTrue(repository.contains("APP_SAMPLE_DIR = \"voice-library/samples\""));
        assertTrue(repository.contains("storesSamplesOutsideProject"));
        assertTrue(useCase.contains("if (!applicationManaged)"));
        assertTrue(useCase.contains("VoiceFileOwnership.USER_APPDATA"));
    }

    @Test
    void documentRefreshesFromActiveVoiceLibraryAndShowsOnlyRegisteredTones() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/VoiceSampleWorkflowCoordinator.java"));

        assertTrue(panel.contains("activeVoiceLibraryProperty().addListener"));
        assertTrue(panel.contains("registeredDocumentTones"));
        assertTrue(panel.contains("sampleSet.registeredTones()"));
        assertTrue(panel.contains("El campo Tono solo mostrará emociones que esa voz ya tenga grabadas o importadas"));
        assertTrue(viewModel.contains("activeVoiceLibrary.set(result.voiceLibrary())"));
        assertTrue(viewModel.contains("activeVoiceLibrary.set(result.voiceLibrary())"));
        assertTrue(workflow.contains("importRecordedToneSample"));
        assertFalse(panel.contains("Tonos recomendados ·"));
    }

    @Test
    void voiceSamplesAreReferencesForGeneratedSpeechNotFixedClips() throws Exception {
        String docs = Files.readString(Path.of("DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md"));
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String editor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java"));
        String overview = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java"));

        assertTrue(docs.contains("referencias para Coqui/XTTS"));
        assertTrue(docs.contains("no son clips fijos"));
        assertTrue((view + editor + overview).contains("no son clips fijos"));
    }
}
