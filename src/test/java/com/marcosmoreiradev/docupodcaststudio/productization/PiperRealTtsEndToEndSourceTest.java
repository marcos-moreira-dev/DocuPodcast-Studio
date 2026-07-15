package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PiperRealTtsEndToEndSourceTest {
    @Test
    void piperHasAConcreteTextFileToWavBridgeAndFactoryWiring() throws Exception {
        String piperTemplate = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/PiperTtsCommandTemplate.java"));
        String piperPolicy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/PiperVoiceModelPathPolicy.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));
        String wrapper = Files.readString(Path.of("scripts/tts/piper-file-to-wav.ps1"));

        assertTrue(piperTemplate.contains("piper-file-to-wav.ps1"));
        assertTrue(piperTemplate.contains("tools/piper/piper.exe"));
        assertTrue(piperTemplate.contains("PiperVoiceModelPathPolicy"));
        assertTrue(piperPolicy.contains("PIPER_VOICES_RELATIVE"));
        assertTrue(factory.contains("PiperTtsCommandTemplate") && factory.contains(".resolve("));
        assertTrue(wrapper.contains("--output_file"));
        assertTrue(wrapper.contains("Get-Content"));
    }

    @Test
    void t85DocumentsPiperAsFirstRealDraftWithoutReplacingMandatoryCoqui() throws Exception {
        String docs = Files.readString(Path.of("docs/productizacion/T85_PIPER_TTS_REAL_END_TO_END.md"));
        String readme = Files.readString(Path.of("README.md"));
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));

        assertTrue(docs.contains("Piper"));
        assertTrue(docs.contains("texto → WAV real"));
        assertTrue(docs.contains("Coqui XTTS sigue siendo obligatorio"));
        assertTrue(readme.contains("Tanda 85"));
        assertTrue(handoff.contains("T85_PIPER_TTS_REAL_END_TO_END.md"));
    }
}
