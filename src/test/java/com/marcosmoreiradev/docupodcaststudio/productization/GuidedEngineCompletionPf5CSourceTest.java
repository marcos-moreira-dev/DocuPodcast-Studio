package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF5C closes the guided in-app preparation surface for voice/media engines. */
final class GuidedEngineCompletionPf5CSourceTest {
    @Test
    void settingsExposesImportActionsForPiperAndFfmpeg() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String piperSettings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java"));
        String videoLocalSettings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java"));
        assertTrue(settings.contains("Importar"));
        assertTrue(settings.contains("Importar"));
        assertTrue(piperSettings.contains("ImportPiperVoiceFolderUseCase"));
        assertTrue(videoLocalSettings.contains("ImportFfmpegRuntimeFolderUseCase"));
        assertFalse(settings.contains("models/tts/piper/voices"));
        assertFalse(settings.contains("tools/ffmpeg/bin"));
    }

    @Test
    void applicationDefinesRealImportUseCases() throws Exception {
        String piper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImportPiperVoiceFolderUseCase.java"));
        String ffmpeg = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/ImportFfmpegRuntimeFolderUseCase.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        assertTrue(piper.contains(".onnx"));
        assertTrue(piper.contains(".onnx.json"));
        assertTrue(ffmpeg.contains("ffmpeg.exe"));
        assertTrue(ffmpeg.contains("ffprobe.exe"));
        assertTrue(services.contains("importPiperVoiceFolder"));
        assertTrue(services.contains("importFfmpegRuntimeFolder"));
    }
}
