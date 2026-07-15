package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class InstallerPortableTp5SourceTest {
    @Test
    void tp5AddsDistributionPlanAndPortableScript() throws Exception {
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildDistributionPackagePlanUseCase.java", "PORTABLE_APP_IMAGE");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/DistributionPackagePlan.java", "toMarkdown");
        assertTrue(Files.exists(Path.of("scripts/32-preparar-app-portable-layout.bat")));
        assertContains("scripts/32-preparar-app-portable-layout.bat", "dist\\portable\\%APP_NAME%");
        assertContains("scripts/14-app-image-completa.bat", "tools/models/scripts copiados");
        assertContains("scripts/15-msi-completo.bat", "Runtime/legal staged");
    }

    @Test
    void tp5DocumentsInstallerAndPortableContract() throws Exception {
        assertContains("docs/productizacion/TP5_INSTALADOR_APP_PORTABLE_REAL.md", "app-image");
        assertContains("docs/productizacion/TP5_INSTALADOR_APP_PORTABLE_REAL.md", "carpeta portable");
        assertContains("docs/productizacion/TP5_INSTALADOR_APP_PORTABLE_REAL.md", "MSI");
        assertContains("docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md", "TP5 — Instalador/app portable real");
    }

    private static void assertContains(String path, String expected) throws Exception {
        String text = Files.readString(Path.of(path));
        assertTrue(text.contains(expected), path + " debe contener: " + expected);
    }
}
