package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class XttsLegacyCommandRepairSourceTest {
    @Test
    void legacyLocalizedAdvancedVoiceCommandIsNormalizedBeforeRuntime() throws Exception {
        String template = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java"));
        String config = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java"));
        String voiceTest = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareVoiceTestSynthesisGateway.java"));
        String script = Files.readString(Path.of("scripts/tts/Voz IA avanzada-file-to-wav.ps1"));

        assertTrue(template.contains("looksLikeLegacyAdvancedVoiceCommand"));
        assertTrue(template.contains("Voz IA avanzada-file-to-wav"));
        assertTrue(template.contains("componentes locales ia avanzada-wrapper"));
        assertTrue(config.contains("normalizeXttsModelDirectoryArgument"));
        assertTrue(config.contains("-ModelDir"));
        assertTrue(voiceTest.contains("effectiveTtsComputePolicy(settings, command, applicationRoot)"));
        assertTrue(script.contains("redirigiendo a xtts-file-to-wav.ps1"));
        assertTrue(script.contains("model-dir-termina-en-model-pth"));
        assertTrue(script.contains("model-normalizado"));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE"));
    }
}
