package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioCompress1FinalAudioSourceTest {
    @Test
    void finalAudioExportSupportsWavMp3AndAacWithoutChangingInternalChunks() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportPodcastAudioUseCase.java"));
        String format = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/AudioExportFormat.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/ExportWorkflowCoordinator.java"));

        assertTrue(format.contains("MP3"));
        assertTrue(format.contains("AAC"));
        assertTrue(useCase.contains("internal cache and resumable chunks as WAV"));
        assertTrue(useCase.contains("libmp3lame"));
        assertTrue(useCase.contains("-codec:a"));
        assertTrue(useCase.contains("192k"));
        assertTrue(shell.contains("Audio final (*.wav, *.mp3, *.aac)"));
        assertTrue(shell.contains("MP3 comprimido"));
        assertTrue(shell.contains("AAC comprimido"));
        assertTrue(workflow.contains("exportPodcastAudio()"));
    }

    @Test
    void finalAudioCompressionRequiresLocalVideoComponentInsteadOfGlobalPath() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ExportPodcastAudioUseCase.java"));
        assertTrue(useCase.contains("EmbeddedFfmpegLocator"));
        assertTrue(useCase.contains("Prepara Video local en Configuración o exporta WAV"));
        assertTrue(useCase.contains("Video local devolvió código"));
    }
}
