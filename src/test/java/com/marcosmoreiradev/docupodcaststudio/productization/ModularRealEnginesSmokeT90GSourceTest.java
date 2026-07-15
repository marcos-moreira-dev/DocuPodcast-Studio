package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModularRealEnginesSmokeT90GSourceTest {
    @Test
    void t90gProvidesModularSmokeScriptsAndRunsOnlySelectedEngines() throws Exception {
        String smoke = Files.readString(Path.of("src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/RealEnginesSmokeScenarioTest.java"));
        String script19 = Files.readString(Path.of("scripts/19-smoke-motores-reales.bat"));
        String coqui = Files.readString(Path.of("scripts/25-smoke-coqui.bat"));
        String piper = Files.readString(Path.of("scripts/26-smoke-piper.bat"));
        String ffmpeg = Files.readString(Path.of("scripts/27-smoke-ffmpeg.bat"));
        String all = Files.readString(Path.of("scripts/28-smoke-motores-producto.bat"));
        String doc = Files.readString(Path.of("docs/productizacion/T90G_SMOKE_MODULAR_MOTORES.md"));

        assertTrue(smoke.contains("config.shouldRun(\"coqui\")"));
        assertTrue(smoke.contains("config.shouldRun(\"piper\")"));
        assertTrue(smoke.contains("config.shouldRun(\"ffmpeg\")"));
        assertTrue(script19.contains("REQUIRED_ENGINES=%~1"));
        assertTrue(script19.contains("-Ddocupodcast.realEnginesSmoke.required=%REQUIRED_ENGINES%"));
        assertTrue(coqui.contains("required=coqui"));
        assertTrue(piper.contains("required=piper"));
        assertTrue(ffmpeg.contains("required=ffmpeg"));
        assertTrue(all.contains("required=coqui,piper,ffmpeg"));
        assertTrue(doc.contains("scripts\\25-smoke-coqui.bat"));
        assertFalse(doc.contains("Audio a texto"));
    }
}
