package com.marcosmoreiradev.docupodcaststudio.infrastructure.settings;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PropertiesOperationalSettingsRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsOperationalSettingsOutsideSourceDocuments() throws Exception {
        Path file = tempDir.resolve("operational-settings.properties");
        PropertiesOperationalSettingsRepository repository = new PropertiesOperationalSettingsRepository(file);
        OperationalSettings settings = new OperationalSettings(
                new OperationalSettings.ReadingDocumentSettings(22, 1.6, true),
                new OperationalSettings.PlaybackBufferSettings(6, 14, true),
                new OperationalSettings.TtsEngineSettings("external", "tts --in {textFile} --out {outputFile}", "Motor local", "es", "VOC-NARRATOR", 300, 3),
                new OperationalSettings.VideoRenderSettings("tools/ffmpeg/bin/ffmpeg.exe", "2K", true),
                new ImageGenerationSettings("managed-local", "http://127.0.0.1:8188", "AUTO", "TEST_4GB_SD15", "sd15.safetensors", "models/image/adapters", 420, true),
                new FrameGenerationSettings("DOUBLE_STOP_MOTION", "SCENE", "exports/frames", "UNIQUE"),
                new OperationalSettings.ComputeSettings("PREFER_GPU", "gpu-0", true, true, "NVIDIA_NVENC"),
                new OperationalSettings.OcrSettings("managed-local", "tools/tesseract/bin/tesseract.exe", "spa+eng", 300, 240, true, "https://example.invalid/tesseract.zip"),
                new OperationalSettings.StorageSettings("models", "exports"),
                new OperationalSettings.DiagnosticSettings(true, true, true));

        repository.save(settings);
        OperationalSettings loaded = repository.load();

        assertTrue(Files.isRegularFile(file));
        assertEquals(22, loaded.readingDocument().baseFontSize());
        assertEquals(14, loaded.playbackBuffer().lookaheadSegments());
        assertEquals("tts --in {textFile} --out {outputFile}", loaded.tts().commandTemplate());
        assertEquals("tools/ffmpeg/bin/ffmpeg.exe", loaded.video().ffmpegExecutable());
        assertEquals("sd15.safetensors", loaded.imageGeneration().modelName());
        assertEquals("SAFE_LOW_VRAM", loaded.imageGeneration().memoryProfile());
        assertEquals("DOUBLE_STOP_MOTION", loaded.frameGeneration().mode());
        assertEquals("exports/frames", loaded.frameGeneration().outputDirectory());
        assertEquals("PREFER_GPU", loaded.compute().policy().name());
        assertEquals("gpu-0", loaded.compute().selectedDeviceId());
        assertEquals("NVIDIA_NVENC", loaded.compute().videoEncoderPolicy().name());
        assertEquals("tools/tesseract/bin/tesseract.exe", loaded.ocr().tesseractExecutable());
        assertEquals("spa+eng", loaded.ocr().languages());
        assertEquals(300, loaded.ocr().dpi());
        assertEquals(240, loaded.ocr().timeoutSeconds());
    }
}
