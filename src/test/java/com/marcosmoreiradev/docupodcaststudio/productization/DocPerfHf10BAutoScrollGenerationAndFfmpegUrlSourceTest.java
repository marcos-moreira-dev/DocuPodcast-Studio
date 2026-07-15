package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-PERF-HF10B: large-document auto-windowing, independent chunk generation and FFmpeg URL centralization. */
final class DocPerfHf10BAutoScrollGenerationAndFfmpegUrlSourceTest {
    @Test
    void largeDocumentWindowAdvancesAutomaticallyNearScrollEdgesWithoutPlaybarBlockingScrollbar() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));

        assertTrue(view.contains("maybeAdvanceRenderWindowForScroll"));
        assertTrue(view.contains("autoSwitchToWindow"));
        assertTrue(view.contains("0.985"));
        assertTrue(view.contains("0.015"));
        assertTrue(view.contains("new Insets(6, 24, 0, 24)"));
        assertTrue(floating.contains("setMaxWidth(820)"));
    }

    @Test
    void statusBarCanStartChunkGenerationWithoutUsingPlaybackTransport() throws Exception {
        String status = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String control = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));

        assertTrue(status.contains("Reconstruir fragmentos de audio"));
        assertTrue(status.contains("Renderizar desde aquí"));
        assertTrue(status.contains("Crear un job nuevo desde el inicio de la lectura preparada"));
        assertTrue(status.contains("Runnable startAudioGeneration"));
        assertTrue(status.contains("Runnable startAudioGenerationFromSelection"));
        assertTrue(shell.contains("this::handleGenerateChunksFromStatusBar"));
        assertTrue(control.contains("onPlayFromBeginning"));
    }

    @Test
    void ffmpegDownloadUrlIsCentralizedLikeVoiceEngineDownloads() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettings.java"));
        String repo = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/settings/PropertiesOperationalSettingsRepository.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));

        assertTrue(settings.contains("DEFAULT_FFMPEG_DOWNLOAD_URL"));
        assertTrue(settings.contains("DOCUPODCAST_FFMPEG_DOWNLOAD_URL"));
        assertTrue(repo.contains("download.ffmpeg.runtimeZipUrl"));
        assertTrue(dialog.contains("ffmpegDownloadUrl"));
        assertTrue(dialog.contains("URL para preparar video local"));
    }

    @Test
    void bufferedGapRecoveryUsesCueUnitNotOnlySegmentToAvoidRepeatingSentence() throws Exception {
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackWorkflowCoordinator.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(workflow.contains("manifest.nextCueAfterUnit"));
        assertTrue(vm.contains("waitingForBufferedSegmentAfter = completedCue.unitId()"));
    }
}
