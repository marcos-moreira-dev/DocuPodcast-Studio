package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThirdPartyManifestTp4SourceTest {
    @Test
    void tp4AddsThirdPartyLicenseManifestAndScript() throws Exception {
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildThirdPartyLicenseManifestUseCase.java", "FFmpeg");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildThirdPartyLicenseManifestUseCase.java", "Piper TTS");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildThirdPartyLicenseManifestUseCase.java", "Coqui/XTTS");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ThirdPartyLicenseManifest.java", "toMarkdown");
        assertTrue(Files.exists(Path.of("scripts/30-generar-manifest-terceros.bat")));
        assertContains("scripts/30-generar-manifest-terceros.bat", "THIRD_PARTY_MANIFEST.md");
    }

    @Test
    void tp4DocumentsLegalBoundaries() throws Exception {
        assertContains("docs/productizacion/TP4_LICENCIAS_MANIFEST_TERCEROS.md", "no asumir derechos de redistribución");
        assertContains("docs/productizacion/TP4_LICENCIAS_MANIFEST_TERCEROS.md", "FFmpeg");
        assertContains("docs/productizacion/TP4_LICENCIAS_MANIFEST_TERCEROS.md", "Piper");
        assertContains("docs/productizacion/TP4_LICENCIAS_MANIFEST_TERCEROS.md", "Coqui/XTTS");
        assertContains("docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md", "TP4 — Licencias");
    }

    private static void assertContains(String path, String expected) throws Exception {
        String text = Files.readString(Path.of(path));
        assertTrue(text.contains(expected), path + " debe contener: " + expected);
    }
}
