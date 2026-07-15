package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** HF5: preparing lightweight voice must not make advanced voice look for a lightweight speaker sample. */
final class AdvancedVoiceLocalSimpleLeakHf5SourceTest {
    @Test
    void advancedVoiceReadinessIgnoresLightweightVoiceId() throws Exception {
        String inspector = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCase.java"));
        String template = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java"));
        String inspectorTest = Files.readString(Path.of("src/test/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCaseTest.java"));

        assertTrue(inspector.contains("advancedSpeakerVoiceId"));
        assertTrue(inspector.contains("voz-local-simple"));
        assertTrue(inspector.contains("return \"voz-por-defecto.wav\""));
        assertTrue(template.contains("advancedSpeakerVoiceId"));
        assertTrue(template.contains("voz-local-simple"));
        assertTrue(inspectorTest.contains("ignoresLocalSimpleVoiceWhenInspectingAdvancedVoiceSetup"));
    }
}
