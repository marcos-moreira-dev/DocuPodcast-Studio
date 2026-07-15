package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GenerationAttemptPolicyTest {
    @Test
    void defaultsUseFourAudioAttemptsAndTwoImageAttempts() {
        GenerationAttemptPolicy policy = GenerationAttemptPolicy.defaults();

        assertEquals(4, policy.maxAttempts(GenerationTaskKind.AUDIO_SEGMENT));
        assertEquals(4, policy.maxAttempts(GenerationTaskKind.AUDIO_BATCH));
        assertEquals(2, policy.maxAttempts(GenerationTaskKind.IMAGE_CANDIDATE));
        assertEquals(2, policy.maxAttempts(GenerationTaskKind.IMAGE_FRAME));
    }

    @Test
    void settingsKeepTtsMaxRetriesCompatibilityAndImageAttempts() {
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("piper", "", "Piper", "es", "VOC-NARRATOR", 180, 3),
                OperationalSettings.VideoRenderSettings.defaults(),
                new ImageGenerationSettings("managed-local", "http://127.0.0.1:8188", "AUTO",
                        "TEST_4GB_SD15", "model.safetensors", "models/image/adapters", 300, true, 5),
                com.marcosmoreiradev.docupodcaststudio.application.settings.FrameGenerationSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        GenerationAttemptPolicy policy = GenerationAttemptPolicy.fromSettings(settings);

        assertEquals(4, policy.maxAttempts(GenerationTaskKind.AUDIO_SEGMENT));
        assertEquals(5, policy.maxAttempts(GenerationTaskKind.IMAGE_BATCH));
    }
}
