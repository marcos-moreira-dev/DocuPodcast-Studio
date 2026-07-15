package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmokeVisualUxRcT112SourceTest {
    @Test
    void t112DocumentsVisualSmokeRcChecklistAndNextProductStep() throws Exception {
        String doc = read("docs/productizacion/T112_SMOKE_VISUAL_UX_RC.md");
        String report = read("docs/productizacion/T112_SMOKE_VISUAL_REPORTE_MANUAL.md");
        String root = read("docs/168_TANDA_112_SMOKE_VISUAL_UX_RC.md");
        String validation = read("VALIDATION.md");
        String handoff = read("AI_HANDOFF.md");

        assertTrue(doc.contains("imagen embebida DOCX"));
        assertTrue(doc.contains("guardado guiado"));
        assertTrue(doc.contains("Mostrar preparación"));
        assertTrue(doc.contains("Voces administra voces"));
        assertTrue(doc.contains("sin Markdown crudo"));
        assertTrue(doc.contains("T113 — Menú Ejemplos"));
        assertTrue(report.contains("Overlay se puede ocultar") || report.contains("overlay se puede ocultar"));
        assertTrue(root.contains("Rail derecho visual/navegacional"));
        assertTrue(validation.contains("Validación T112"));
        assertTrue(handoff.contains("Base vigente: T112"));
    }

    @Test
    void t112KeepsT111IconographyHotfixAndTransversalComponentRule() throws Exception {
        String ribbonCss = read("src/main/resources/css/components/ribbon.css");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");
        String overlay = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java");
        String handoff = read("AI_HANDOFF.md");

        assertTrue(ribbonCss.contains("T111-HF2 — PNG icon polish"));
        assertTrue(ribbonCss.contains("T111-HF4 — larger PNG icon polish"));
        assertTrue(ribbon.contains("RibbonIconCatalog.iconFor(command.commandId())"));
        assertTrue(status.contains("Mostrar preparación"));
        assertTrue(overlay.contains("Ocultar"));
        assertTrue(handoff.contains("componentes GUI transversales"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
