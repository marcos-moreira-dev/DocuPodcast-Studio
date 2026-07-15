package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-HF8R makes chunk playback wait for the real WAV duration instead of text estimates. */
final class PlaybackDurationQueueHf8RSourceTest {
    @Test
    void generatedWavsStoreMeasuredDurationInsteadOfTextEstimate() throws Exception {
        String gateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java");
        assertTrue(gateway.contains("WavAudioDurationProbe"));
        assertTrue(gateway.contains("durationProbe.durationSeconds(outputFile)"));
        assertTrue(gateway.contains("measuredDuration"));
        assertFalse(gateway.contains("estimatedDuration(segment)"), "El WAV real no debe guardar duración estimada por cantidad de texto.");
    }

    @Test
    void persistedJobsRepairOldDurationsFromExistingWavs() throws Exception {
        String repository = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/AudioJobFileRepository.java");
        assertTrue(repository.contains("withMeasuredDuration"));
        assertTrue(repository.contains("durationProbe.tryDurationSeconds"));
        assertTrue(repository.contains("resolveAudioPath"));
    }

    @Test
    void shellUsesCueClockBeforeAdvancingToNextChunk() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String controller = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java");
        String transport = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java");
        assertTrue(controller.contains("PlaybackCueClock"));
        assertTrue(controller.contains("clock.completed"));
        assertTrue(shell.contains("playbackTransport.continuationActive()"));
        assertTrue(transport.contains("continuation.active()"));
        assertTrue(controller.contains("blockingCurrentCue"));
        assertTrue(shell.lines().count() <= 2600, "DocuPodcastShellViewModel debe seguir bajo el limite RF2.");
    }

    @Test
    void coquiDownloadRemainsAnExplicitManualSmokeNotSilentStartupWork() throws Exception {
        String downloader = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java");
        assertTrue(downloader.contains("https://huggingface.co/coqui/XTTS-v2"));
        assertTrue(downloader.contains("normalizeRepositoryUrlForDisplay"));
        assertFalse(downloader.contains("OFFICIAL_MODEL_BASE_URL = \"https://huggingface.co/coqui/XTTS-v2/resolve/main/\""));
        assertTrue(downloader.contains("never runs silently"));
        assertTrue(downloader.contains("Voz IA avanzada descargada y verificada dentro del programa"));
    }

    private static String read(String file) throws Exception {
        return Files.readString(Path.of(file));
    }
}
