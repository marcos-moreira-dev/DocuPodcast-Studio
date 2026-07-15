package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FlexibleMediaInputBrainSourceTest {
    @Test
    void brainAcceptsMp3WavAndVideoAsAudioSourceWithoutSplittingUserActions() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/media/UserMediaFormatPolicy.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/media/ImportUserMediaAssetUseCase.java"));
        String kinds = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/assets/ProjectAssetKind.java"));

        assertTrue(policy.contains("mp3"));
        assertTrue(policy.contains("wav"));
        assertTrue(policy.contains("mp4"));
        assertTrue(policy.contains("webm"));
        assertTrue(useCase.contains("VIDEO_AUDIO_EXTRACTED"));
        assertTrue(useCase.contains("FFmpeg"));
        assertTrue(kinds.contains("VIDEO_SOURCE"));
    }

    @Test
    void documentationKeepsTechnicalDetailsOutOfMainReader() throws Exception {
        String doc = Files.readString(Path.of("docs/productizacion/MEDIA_INPUT_FLEXIBLE_T80C.md"));
        assertTrue(doc.contains("Asignar audio"));
        assertTrue(doc.contains("MP3"));
        assertTrue(doc.contains("WAV"));
        assertTrue(doc.contains("video"));
        assertTrue(doc.contains("Configuración"));
        assertTrue(doc.contains("La acción es **Asignar audio**"));
    }
}
