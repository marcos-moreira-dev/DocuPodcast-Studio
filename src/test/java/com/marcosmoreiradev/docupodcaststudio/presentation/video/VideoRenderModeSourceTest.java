package com.marcosmoreiradev.docupodcaststudio.presentation.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoRenderModeSourceTest {
    @Test
    void videoRenderUsesEmbeddedFfmpegAndBlockingProgressView() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ExportSimpleVideoPackageUseCase.java"));
        String locator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/EmbeddedFfmpegLocator.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String formModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java"));
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoRenderProgressView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/render-progress.css"));

        assertTrue(useCase.contains("SimpleVideoExportSettings.defaults()"));
        assertTrue(useCase.contains("FFMPEG_RENDER_CONTRACT.md"));
        assertTrue(useCase.contains("tools\\\\ffmpeg\\\\bin\\\\ffmpeg.exe") || useCase.contains("tools/ffmpeg/bin/ffmpeg.exe"));
        assertTrue(locator.contains("tools/ffmpeg/bin/ffmpeg.exe"));
        assertTrue(settings.contains("Resolución por defecto") || settings.contains("Resolución"));
        assertTrue(settings.contains("720p, 1080p, 2K y 4K")
                || formModel.contains("combo(\"2K\", \"720P\", \"1080P\", \"2K\", \"4K\")"));
        assertTrue(settings.contains("Bloquear lectura, edición y nuevas exportaciones"));
        assertTrue(view.contains("ProgressBar"));
        assertTrue(view.contains("Cancelar render"));
        assertTrue(css.contains("video-render-progress"));
    }
}
