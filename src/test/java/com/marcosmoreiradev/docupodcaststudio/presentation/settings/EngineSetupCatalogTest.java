package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EngineSetupCatalogTest {
    @Test
    void recommendedCatalogOffersHighQualityVoiceAndLightweightFallbackWithoutFixedUrlDependency() {
        var options = EngineSetupCatalog.recommendedOptions();
        assertEquals(3, options.size());

        String titles = options.stream().map(EngineSetupOption::title).collect(Collectors.joining(" | "));
        assertTrue(titles.contains("Voz IA avanzada"));
        assertTrue(titles.contains("Voz local simple"));
        assertTrue(titles.contains("Imagen IA teatral local"));
        assertFalse(titles.contains("Whisper"));

        String all = options.toString();
        assertTrue(all.contains("Importar modelo manualmente"));
        assertTrue(all.contains("Importar paquete local"));
        assertTrue(all.contains("checksum"));
        assertTrue(all.contains("línea de comandos"));
        assertFalse(all.contains("https://"));
        assertFalse(all.contains("http://"));
    }
}
