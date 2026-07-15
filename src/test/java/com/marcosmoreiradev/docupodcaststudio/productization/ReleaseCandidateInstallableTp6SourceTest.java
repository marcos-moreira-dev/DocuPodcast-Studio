package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleaseCandidateInstallableTp6SourceTest {
    @Test
    void tp6AddsRcGatesAndSmokeScript() throws Exception {
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ReleaseCandidateGate.java", "defaultGates");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ReleaseCandidateGate.java", "visual-smoke");
        assertTrue(Files.exists(Path.of("scripts/33-smoke-rc-instalable.bat")));
        assertContains("scripts/33-smoke-rc-instalable.bat", "TP6_RC_SMOKE_REPORT.md");
        assertContains("scripts/16-release-candidate.bat", "30-generar-manifest-terceros.bat");
        assertContains("scripts/16-release-candidate.bat", "32-preparar-app-portable-layout.bat");
        assertContains("scripts/16-release-candidate.bat", "33-smoke-rc-instalable.bat");
    }

    @Test
    void tp6DocumentsInstallableRcEvidence() throws Exception {
        assertContains("docs/productizacion/TP6_RC_INSTALABLE_SMOKE.md", "diagnóstico completo verde");
        assertContains("docs/productizacion/TP6_RC_INSTALABLE_SMOKE.md", "third-party manifest");
        assertContains("docs/productizacion/TP6_RC_INSTALABLE_SMOKE.md", "app-image/portable");
        assertContains("README.md", "Base vigente: TP6");
        assertContains("AI_HANDOFF.md", "Base vigente: TP6");
        assertContains("VALIDATION.md", "Base vigente: TP6");
    }

    private static void assertContains(String path, String expected) throws Exception {
        String text = Files.readString(Path.of(path));
        assertTrue(text.contains(expected), path + " debe contener: " + expected);
    }
}
