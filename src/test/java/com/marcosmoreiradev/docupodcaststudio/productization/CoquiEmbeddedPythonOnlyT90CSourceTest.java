package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CoquiEmbeddedPythonOnlyT90CSourceTest {
    @Test
    void xttsTemplateDoesNotFallbackToGlobalPythonOrPath() throws Exception {
        String template = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java"));
        assertTrue(template.contains("tools/xtts-wrapper/.venv/Scripts/python.exe"));
        assertTrue(template.contains("must never fall back to a global Python installation"));
        assertFalse(template.contains("Path.of(\"python\")"));
        assertFalse(template.contains("Path.of(\"python3\")"));
        assertFalse(template.contains("Path.of(\"py\")"));
    }

    @Test
    void smokeAndBridgeUseOnlyRepoLocalPythonForCoqui() throws Exception {
        String smoke = Files.readString(Path.of("src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/RealEnginesSmokeScenarioTest.java"));
        String bridge = Files.readString(Path.of("scripts/tts/xtts-file-to-wav.ps1"));
        String doc = Files.readString(Path.of("docs/productizacion/T90_SMOKE_REAL_MOTORES.md"));
        assertTrue(smoke.contains("tools/xtts-wrapper/.venv/Scripts/python.exe"));
        assertFalse(smoke.contains("DOCUPODCAST_XTTS_PYTHON"));
        assertFalse(smoke.contains("docupodcast.smoke.xtts.python"));
        assertTrue(bridge.contains("Python no permitido para Coqui/XTTS"));
        assertTrue(bridge.contains("No se usa Python global"));
        assertTrue(doc.contains("No se permite sobrescribirlo con Python global ni PATH"));
    }
}
