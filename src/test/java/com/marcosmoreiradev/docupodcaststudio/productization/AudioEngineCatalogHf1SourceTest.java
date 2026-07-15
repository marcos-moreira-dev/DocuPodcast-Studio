package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** AUDIO-ENGINE-CATALOG-HF1 keeps Documento honest about usable audio origins. */
final class AudioEngineCatalogHf1SourceTest {
    @Test
    void documentUsesAvailabilityCatalogInsteadOfOnlyActiveEngineDescriptor() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String service = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/AudioApplicationServices.java");

        assertTrue(panel.contains("documentAudioSourceAvailability()"));
        assertTrue(panel.contains("AudioEngineAvailability::usableInDocument"));
        assertTrue(viewModel.contains("selectDocumentAudioSource"));
        assertTrue(service.contains("ListAudioEngineAvailabilityUseCase"));
    }

    @Test
    void catalogMentionsOnlyOperativeChoicesForDocumentSourceSelector() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/ListAudioEngineAvailabilityUseCase.java");
        String availability = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioEngineAvailability.java");

        assertTrue(catalog.contains("canGenerateDocumentAudio"));
        assertTrue(catalog.contains("PiperSetupReadinessReport"));
        assertTrue(availability.contains("usableInDocument"));
        assertTrue(availability.contains("Audio del computador"));
        assertTrue(availability.contains("Modo de prueba"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
