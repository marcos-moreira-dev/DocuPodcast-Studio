package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CoquiXttsDefaultVoiceSourceTest {
    @Test
    void coquiXttsWrapperAndDefaultVoiceSampleAreDocumentedAndVersioned() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("scripts/tts/xtts-file-to-wav.ps1")));
        assertTrue(Files.isRegularFile(Path.of("tools/xtts-wrapper/synthesize_xtts.py")));
        assertTrue(Files.isRegularFile(Path.of("models/tts/xtts/speakers/voz-por-defecto.wav")));
        assertTrue(Files.isRegularFile(Path.of("samples/voices/advanced-presets/hombre_adulto_personaje_narrativo/neutral.wav")));
        assertTrue(Files.isRegularFile(Path.of("samples/theatre/maps/mapa-espacial.png")));

        String plan = Files.readString(Path.of("docs/productizacion/T86_COQUI_XTTS_WRAPPER_VOZ_DEFECTO.md"));
        assertTrue(plan.contains("Coqui XTTS"));
        assertTrue(plan.contains("voz-por-defecto.wav"));
        assertTrue(plan.contains("samples/voices/advanced-presets"));
        assertTrue(plan.contains("presets oficiales"));
        assertTrue(plan.contains("no descarga modelos automáticamente"));
    }

    @Test
    void settingsAwareGatewayDerivesAdvancedVoiceCommandBeforeFallingBackToSimpleVoice() throws Exception {
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java"));
        assertTrue(gateway.contains("XttsTtsCommandTemplate.resolve"));
        assertTrue(gateway.contains("PiperTtsCommandTemplate.resolve"));
        assertTrue(gateway.indexOf("XttsTtsCommandTemplate.resolve") < gateway.indexOf("PiperTtsCommandTemplate.resolve"));
    }
}
