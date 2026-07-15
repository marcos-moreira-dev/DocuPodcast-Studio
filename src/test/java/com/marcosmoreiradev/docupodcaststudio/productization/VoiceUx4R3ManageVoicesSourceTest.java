package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-3: Gestionar voces must create/delete/export and replace emotion samples soberly. */
final class VoiceUx4R3ManageVoicesSourceTest {
    @Test
    void manageVoicesExposesSoberCreateDeleteExportAndReplacementFlow() throws IOException {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String header = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManagementHeaderPanel.java"));
        String editor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java"));
        String actions = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java"));
        assertTrue(view.contains("Nueva voz"));
        assertTrue(view.contains("Guardar / actualizar voz"));
        assertTrue(view.contains("Eliminar voz"));
        assertTrue(view.contains("Exportar muestras"));
        assertTrue(view.contains("VoiceProfilePresentationPolicy.predefinedVoice"));
        assertTrue(header.contains("Voces registradas"));
        assertTrue(editor.contains("Importar o grabar reemplaza la muestra de la emoción seleccionada"));
        assertTrue(editor.contains("Emociones listas"));
        assertTrue(actions.contains("markSamplePlaybackFinished"));
        assertTrue(actions.contains("viewModel.importVoiceSample"));
        assertTrue(actions.contains("viewModel.startVoiceRecording"));
    }

    @Test
    void voiceDeletionWarnsAboutAudioSamplesAndUsesCoordinator() throws IOException {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/VoiceProfileAdministrationCoordinator.java"));
        assertTrue(view.contains("También se retirarán sus emociones registradas"));
        assertTrue(shell.contains("deleteVoiceProfileImpactLabel"));
        assertTrue(shell.contains("voiceProfileAdministration.delete"));
        assertTrue(coordinator.contains("withoutVoice"));
        assertTrue(coordinator.contains("removedSamples"));
    }

    @Test
    void voiceLibrarySupportsRemovingVoiceWithSampleSet() throws IOException {
        String library = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceLibrary.java"));
        assertTrue(library.contains("withoutVoice(String voiceProfileId)"));
        assertTrue(library.contains("updatedSamples"));
    }
}
