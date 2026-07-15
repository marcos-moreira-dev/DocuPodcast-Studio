package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegPrep1DownloadPreparationSourceTest {
    @Test
    void settingsPreparationDownloadsConfiguredVideoRuntimeInsteadOfOnlyImportingFolder() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String videoLocal = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");

        assertTrue(dialog.contains("downloadFfmpegPortableRuntime"));
        assertTrue(videoLocal.contains("DownloadFfmpegPortableRuntimeUseCase"));
        assertTrue(videoLocal.contains("Descargar y verificar video local"));
        assertFalse(dialog.contains("Este botón todavía no descarga desde internet"));
        assertTrue(services.contains("DownloadFfmpegPortableRuntimeUseCase downloadFfmpegPortableRuntime"));
        assertTrue(factory.contains("new DownloadFfmpegPortableRuntimeUseCase"));
        assertTrue(factory.contains("ffmpegProbe"));
    }

    @Test
    void videoRuntimeDownloaderExtractsZipAndVerifiesFinalVideoReadiness() throws Exception {
        String downloader = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/DownloadFfmpegPortableRuntimeUseCase.java");
        String report = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/FfmpegRuntimeDownloadReport.java");

        assertTrue(downloader.contains("HttpClient"));
        assertTrue(downloader.contains("ZipInputStream"));
        assertTrue(downloader.contains("tools/ffmpeg"));
        assertTrue(downloader.contains("ffmpeg.exe"));
        assertTrue(downloader.contains("ffprobe.exe"));
        assertTrue(downloader.contains("readyForFinalVideo"));
        assertTrue(downloader.contains("DEFAULT_FFMPEG_DOWNLOAD_URL"));
        assertTrue(report.contains("runtimeAfter"));
        assertTrue(report.contains("failedItems"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
