package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-OPERATIVE-SURFACES-RC1 documents the rule: visible UI exists for action, state or decision. */
final class OperationalSurfacesRc1SourceTest {
    @Test
    void productizationDocStatesOperationalSurfaceRule() throws Exception {
        String doc = read("docs/productizacion/PROJECT_INTEGRITY_UX1_GUIDE_UX_HF1_OPERATIVE_SURFACES_RC1.md");
        assertTrue(doc.contains("acción, decisión, estado, advertencia o siguiente paso"));
        assertTrue(doc.contains("No se agregan tarjetas, métricas ni textos para aparentar dashboard"));
    }

    @Test
    void guideAvoidsDecorativeDashboardLanguage() throws Exception {
        String all = Files.list(Path.of("src/main/resources/help/topics"))
                .filter(path -> path.toString().endsWith(".md"))
                .map(path -> { try { return Files.readString(path); } catch (Exception ex) { throw new RuntimeException(ex); } })
                .reduce("", (left, right) -> left + "\n" + right);
        assertFalse(all.contains("Guía informativa, no botón"));
        assertFalse(all.contains("dashboard decorativo"));
        assertTrue(all.contains("Acciones") || all.contains("Pasos"));
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }
}
