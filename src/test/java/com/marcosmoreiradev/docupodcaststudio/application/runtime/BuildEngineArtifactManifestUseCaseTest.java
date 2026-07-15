package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildEngineArtifactManifestUseCaseTest {
    @Test
    void manifestListsRequiredCoquiAndFfmpegArtifactsForFinalRc() {
        EngineArtifactManifest manifest = new BuildEngineArtifactManifestUseCase().execute();
        String joined = manifest.artifacts().toString();

        assertTrue(joined.contains("ffmpeg.exe"));
        assertTrue(joined.contains("ffprobe.exe"));
        assertTrue(joined.contains(".venv/Scripts/python.exe"));
        assertTrue(joined.contains("synthesize_xtts.py"));
        assertTrue(joined.contains("config.json"));
        assertTrue(joined.contains("*.pth|*.safetensors"));
        assertTrue(joined.contains("voz-por-defecto.wav"));
        assertTrue(manifest.requiredForFinalRc().stream().anyMatch(a -> a.id().equals("ffmpeg-exe")));
        assertTrue(manifest.requiredForFinalRc().stream().anyMatch(a -> a.id().equals("xtts-python")));
    }

    @Test
    void finalRcValidationFailsUntilChecksumsAreConcrete() {
        EngineArtifactManifest manifest = new BuildEngineArtifactManifestUseCase().execute();
        EngineArtifactValidationReport report = new ValidateEngineArtifactManifestUseCase().validateForFinalRc(manifest);

        assertFalse(report.readyForFinalRc());
        assertFalse(report.issues().isEmpty());
        assertTrue(report.issues().stream().anyMatch(issue -> issue.contains("SHA-256")));
    }
}
