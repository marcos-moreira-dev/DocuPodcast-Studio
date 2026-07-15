package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RuntimeArtifactPathsTest {
    @Test
    void resolvesCanonicalXttsAndVoiceLibraryPaths() {
        Path root = Path.of("/tmp/DocuPodcast").toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        assertEquals(root.resolve("models/tts/xtts").normalize(), paths.xttsModelDirectory());
        assertEquals(root.resolve("runtime/tts/xtts-smoke/xtts-cuda-smoke.json").normalize(), paths.xttsCudaSmokeManifest());
        assertEquals(root.resolve("tools/tesseract/bin/tesseract.exe").normalize(), paths.tesseractExecutable());
        assertEquals(root.resolve("voice-library/samples").normalize(), paths.voiceSamplesDirectory());
        assertEquals(root.resolve("voice-library/tmp-recordings").normalize(), paths.voiceTempRecordingsDirectory());
    }
}
