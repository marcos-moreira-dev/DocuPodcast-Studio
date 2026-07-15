package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T119C guards engine artifact manifests, licenses and checksums before RC final. */
final class EngineArtifactsManifestT119CSourceTest {
    @Test
    void applicationDefinesEngineArtifactManifestContract() throws Exception {
        String manifest = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildEngineArtifactManifestUseCase.java");
        String descriptor = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/EngineArtifactDescriptor.java");
        String validator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ValidateEngineArtifactManifestUseCase.java");

        assertTrue(manifest.contains("ffmpeg.exe"));
        assertTrue(manifest.contains("ffprobe.exe"));
        assertTrue(manifest.contains(".venv/Scripts/python.exe"));
        assertTrue(manifest.contains("synthesize_xtts.py"));
        assertTrue(manifest.contains("models/tts/xtts/config.json"));
        assertTrue(manifest.contains("*.pth|*.safetensors"));
        assertTrue(manifest.contains("voz-por-defecto.wav"));
        assertTrue(manifest.contains("piper.exe"));
        assertTrue(manifest.contains("*.onnx.json"));
        assertTrue(descriptor.contains("PENDING_SHA256_UNTIL_ARTIFACT_LOCKED"));
        assertTrue(validator.contains("falta SHA-256 concreto"));
    }

    @Test
    void scriptsGenerateHumanReadableEngineArtifactManifest() throws Exception {
        String script = read("scripts/30-generar-manifest-terceros.bat");
        assertTrue(script.contains("ENGINE_ARTIFACTS_MANIFEST.md"));
        assertTrue(script.contains("ffmpeg.exe"));
        assertTrue(script.contains("ffprobe.exe"));
        assertTrue(script.contains("voz-por-defecto.wav"));
        assertFalse(script.contains("Whisper"));
        assertFalse(script.contains("STT"));
    }

    @Test
    void documentationRecordsT119CManifestRule() throws Exception {
        String docs = read("docs/productizacion/T119C_MANIFIESTOS_LICENCIAS_CHECKSUMS_MOTORES.md");
        assertTrue(docs.contains("ENGINE_ARTIFACTS_MANIFEST"));
        assertTrue(docs.contains("SHA-256"));
        assertTrue(docs.contains("Coqui/XTTS"));
        assertTrue(docs.contains("FFmpeg"));
        assertTrue(docs.contains("Piper"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
