package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegMediaT88SourceTest {
    @Test
    void t88AddsFfmpegNormalizationAndConnectsDocumentComputerAudioActions() throws Exception {
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/media/UserMediaFormatPolicy.java");
        String importUseCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/media/ImportUserMediaAssetUseCase.java");
        String audioGateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/media/FfmpegAudioNormalizationGateway.java");
        String videoGateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/media/FfmpegVideoAudioExtractionGateway.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String audioPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");
        String shellView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");

        assertTrue(policy.contains("m4a"));
        assertTrue(policy.contains("flac"));
        assertTrue(policy.contains("ogg"));
        assertTrue(importUseCase.contains("AUDIO_NORMALIZED"));
        assertTrue(importUseCase.contains("AudioNormalizationProfile.ASSIGNABLE_AUDIO"));
        assertFalse(audioGateway.contains("AudioNormalizationProfile.SPEECH_TO_TEXT"));
        assertTrue(videoGateway.contains("ffmpeg-extraction"));
        assertTrue(viewModel.contains("importUserAudioForSelectedDocumentRange"));
        assertTrue(viewModel.contains("extractVideoAudioForSelectedDocumentRange"));
        assertTrue(audioPanel.contains("FileChooser"));
        assertTrue(audioPanel.contains("importUserAudioForSelectedDocumentRange"));
        assertTrue(audioPanel.contains("extractVideoAudioForSelectedDocumentRange"));
        assertTrue(shellView.contains("importUserAudioForSelectedDocumentRange"));
        assertFalse(shellView.contains("importAudioForSelectedDocumentRange"));
        String chooseSection = audioPanel.substring(Math.max(0, audioPanel.indexOf("Button choose")));
        assertFalse(chooseSection.contains("prepareDocumentLayerAssignment(NarrativeLayerKind.HUMAN_AUDIO)"));
    }

    @Test
    void t88DocumentationExists() throws Exception {
        String doc = read("docs/productizacion/T88_FFMPEG_MEDIA_REAL.md");
        assertTrue(doc.contains("Elegir audio"));
        assertTrue(doc.contains("Extraer audio de video"));
        assertTrue(doc.contains("WAV/MP3/M4A/FLAC/OGG"));
        assertTrue(doc.contains("Audio del computador"));
        assertFalse(doc.contains("Whisper como núcleo"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
