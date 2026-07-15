package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF1 guardrail: the Voices generated-test path must not synthesize fake WAVs in productive code. */
final class VoiceGeneratedTestRealSynthesisPF1SourceTest {
    @Test
    void generatedVoiceTestUsesRealSynthesisPortInsteadOfAudiblePlaceholder() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/GenerateVoiceTestUseCase.java"));
        String servicesFactory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java"));
        String infrastructure = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalProcessVoiceTestSynthesisGateway.java"));
        String configuration = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java"));

        assertTrue(useCase.contains("VoiceTestSynthesisGateway"));
        assertTrue(useCase.contains("VoiceTestSynthesisRequest"));
        assertTrue(useCase.contains("realSynthesis"));
        assertTrue(servicesFactory.contains("infrastructure.voiceTestSynthesisGateway()"));
        assertTrue(infrastructure.contains("ExternalProcessRunner"));
        assertTrue(infrastructure.contains("ExternalProcessRequest"));
        assertTrue(configuration.contains("commandForVoiceTest"));
        assertTrue(configuration.contains("{speakerWav}"));
        assertFalse(useCase.contains("writeAudiblePlaceholder"));
        assertFalse(useCase.contains("Math.sin"));
        assertFalse(useCase.contains("SAMPLE_RATE"));
    }

    @Test
    void xttsWrapperLoadsLocalModelFolderWithoutAutomaticDownloadFallback() throws Exception {
        String wrapper = Files.readString(Path.of("tools/xtts-wrapper/synthesize_xtts.py"));

        assertTrue(wrapper.contains("model_root = resolve_model_root(args.model_dir)"));
        assertTrue(wrapper.contains("model_path = model_root / \"model.pth\""));
        assertTrue(wrapper.contains("config_path = model_root / \"config.json\""));
        assertTrue(wrapper.contains("TTS(model_path=str(model_root), config_path=str(config_path)"));
        assertTrue(wrapper.contains("DocuPodcast does not download XTTS models automatically"));
        assertFalse(wrapper.contains("tts_models/multilingual/multi-dataset/xtts_v2"));
        assertFalse(wrapper.contains("TTS(model_path=str(model_path)"));
    }
}
