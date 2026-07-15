package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF3A: Voice Library must manage real tone samples, not only display cards. */
final class VoiceLibraryOperationalSamplesPf3ASourceTest {
    @Test
    void voiceLibraryExposesRealSampleActions() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String voices = view + actions;
        assertTrue(view.contains("VoiceSampleActions"));
        assertTrue(voices.contains("Reproducir muestra"));
        assertTrue(voices.contains("Exportar muestra"));
        assertTrue(voices.contains("Eliminar muestra"));
        assertTrue(actions.contains("playSelectedToneSample"));
        assertTrue(actions.contains("exportSelectedToneSample"));
        assertTrue(actions.contains("deleteSelectedToneSample"));
        assertTrue(view.contains("Documento usa voces listas"));
    }

    @Test
    void shellAndServicesWireSamplePlaybackExportDelete() throws Exception {
        String vm = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/VoiceApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        assertTrue(vm.contains("playVoiceReferenceSample"));
        assertTrue(vm.contains("downloadVoiceReferenceSample"));
        assertTrue(vm.contains("deleteVoiceReferenceSample"));
        assertTrue(services.contains("DownloadVoiceReferenceSampleUseCase"));
        assertTrue(services.contains("DeleteVoiceReferenceSampleUseCase"));
        assertTrue(factory.contains("new DownloadVoiceReferenceSampleUseCase"));
        assertTrue(factory.contains("new DeleteVoiceReferenceSampleUseCase"));
    }

    @Test
    void userVoiceIsNotPresentedAsPlaceholder() throws Exception {
        String voiceProfile = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceProfile.java");
        String officialCatalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalog.java");
        assertFalse(voiceProfile.contains("\"Mi voz\""));
        assertFalse(voiceProfile.contains("Mi voz - pendiente de muestra"));
        assertTrue(officialCatalog.contains("Hombre adulto narrativo"));
        assertTrue(officialCatalog.contains("officialPreset"));
        assertTrue(officialCatalog.contains("builtInAdvancedReference"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
