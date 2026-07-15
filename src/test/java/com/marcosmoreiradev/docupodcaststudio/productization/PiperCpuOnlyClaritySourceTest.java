package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PiperCpuOnlyClaritySourceTest {
    @Test
    void localSimpleVoiceExplainsThatDeviceIsForwardedToPiperRuntime() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/ListAudioEngineAvailabilityUseCase.java"));
        String script = Files.readString(Path.of("scripts/tts/piper-file-to-wav.ps1"));
        assertTrue(catalog.contains("Voz local simple/Piper recibe el dispositivo solicitado"));
        assertTrue(catalog.contains("depende del binario local"));
        assertTrue(script.contains("DOCUPODCAST_PIPER_DEVICE"));
        assertTrue(script.contains("HIP_VISIBLE_DEVICES"));
        assertTrue(script.contains("ONEAPI_DEVICE_SELECTOR"));
    }
}
