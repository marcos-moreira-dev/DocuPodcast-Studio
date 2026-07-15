package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the in-app embedded dependency assistant for voice, CUDA, video and OCR. */
final class EmbeddedDependencyAssistantSourceTest {
    @Test
    void recommendedSetupCoversVoiceCudaPiperEmbeddedFfmpegAndOcr() throws Exception {
        String assistant = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EmbeddedDependencySetupAssistant.java");

        assertTrue(assistant.contains("prepareXtts"));
        assertTrue(assistant.contains("prepareCudaIfNeeded"));
        assertTrue(assistant.contains("preparePiper"));
        assertTrue(assistant.contains("prepareFfmpeg"));
        assertTrue(assistant.contains("prepareOcr"));
        assertTrue(assistant.contains("OCR PDF local"));
        assertTrue(assistant.contains("DownloadTesseractPortableRuntimeUseCase"));
        assertTrue(assistant.contains("tools/ffmpeg/bin"));
        assertTrue(assistant.contains("ffmpeg.exe"));
        assertTrue(assistant.contains("ffprobe.exe"));
        assertTrue(assistant.contains("saveEmbeddedFfmpegSettings"));
    }

    @Test
    void startupAndSettingsExposeRecommendedDependencyPreparation() throws Exception {
        String app = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/DocuPodcastStudioApp.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");

        assertTrue(app.contains("runStartupDependencyPreflight"));
        assertTrue(shell.contains("EmbeddedDependencySetupAssistant dependencySetupAssistant"));
        assertTrue(shell.contains("dependencySetupAssistant.runStartupPreflight"));
        assertTrue(settings.contains("Preparar dependencias recomendadas"));
    }

    @Test
    void videoExportOffersEmbeddedFfmpegSetupAndRetriesAfterSuccess() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String assistant = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EmbeddedDependencySetupAssistant.java");

        assertTrue(shell.contains("offerVideoLocalSetupIfMissing"));
        assertTrue(shell.contains("() -> exportFinalVideoInBackground(file, options)"));
        assertTrue(shell.contains("() -> exportTheatreSpatialVideoInBackground(file, options)"));
        assertTrue(assistant.contains("Video local no esta preparado"));
        assertTrue(assistant.contains("runVideoLocalSetup"));
        assertTrue(assistant.contains("embeddedVideoReady"));
    }

    @Test
    void playbackDiagnosticsAreRecordedWithoutRetryPolicy() throws Exception {
        String recorder = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackDiagnosticRecorder.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(recorder.contains("playback-diagnostics.jsonl"));
        assertTrue(recorder.contains("never controls playback behavior"));
        assertTrue(shell.contains("\"cue-play-start\""));
        assertTrue(shell.contains("recordPlaybackEvent(\"player-finished-callback\""));
        assertTrue(shell.contains("recordPlaybackEvent(\"cue-advance\""));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
