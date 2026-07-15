package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RenderUnitEndToEndTi1SourceTest {
    @Test
    void ti1AddsRenderUnitContractAndService() throws Exception {
        String unit = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/render/RenderUnit.java");
        String kind = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/render/RenderUnitKind.java");
        String plan = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/render/RenderUnitPlan.java");
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/render/BuildRenderUnitPlanUseCase.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/RenderApplicationServices.java");

        assertTrue(kind.contains("SPOKEN_ONLY"));
        assertTrue(kind.contains("SPOKEN_WITH_VISUAL"));
        assertTrue(kind.contains("VISUAL_SILENT"));
        assertTrue(unit.contains("visualSilent"));
        assertTrue(unit.contains("renderableInVideo"));
        assertTrue(plan.contains("silentVisualUnitCount"));
        assertTrue(useCase.contains("OperationalSettings.VideoRenderSettings"));
        assertTrue(services.contains("BuildRenderUnitPlanUseCase"));
    }

    @Test
    void ti1DocumentsRemainingRoadmapExhaustively() throws Exception {
        String roadmap = read("docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md");
        String doc = read("docs/productizacion/TI1_RENDERUNIT_END_TO_END.md");
        String handoff = read("AI_HANDOFF.md");

        assertTrue(doc.contains("RenderUnit end-to-end"));
        assertTrue(roadmap.contains("TI2 — Audio jobs desde RenderPlan"));
        assertTrue(roadmap.contains("TI3 — Storyboard/video desde RenderPlan"));
        assertTrue(roadmap.contains("TI4 — Bloques visuales no narrables"));
        assertTrue(roadmap.contains("TI5 — TextAnchor fuerte y reconciliación"));
        assertTrue(roadmap.contains("TI6 — FFmpeg como job persistente/cancelable"));
        assertTrue(roadmap.contains("TI7 — Playback por oración/unidad"));
        assertTrue(roadmap.contains("RF1 — Dividir `DocuPodcastShellViewModel`"));
        assertTrue(roadmap.contains("RF5 — Limpieza final STT/Whisper"));
        assertTrue(handoff.contains("Base vigente: TI1"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
