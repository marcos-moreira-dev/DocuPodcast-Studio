package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepRoadmapAndRefreshSourceTest {
    @Test
    void t71DocumentsRemainingBrainTandasAndRefreshesThroughUnifiedSourceService() throws Exception {
        String roadmap = Files.readString(Path.of("docs/productizacion/TANDAS_PENDIENTES_PROFUNDAS_POST_T71.md"));
        String refresh = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/RefreshSourceDocumentUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/document/SourceDocumentChangeReport.java"));

        assertTrue(roadmap.contains("T72 — Escuchar documento end-to-end"));
        assertTrue(roadmap.contains("T73 — Audio jobs robustos"));
        assertTrue(roadmap.contains("T74 — Capas narrativas reales"));
        assertTrue(roadmap.contains("T75 — Storyboard como capa del documento"));
        assertTrue(roadmap.contains("T76 — Video package / render contract"));
        assertTrue(roadmap.contains("T77 — Integridad y reparación del proyecto"));
        assertTrue(roadmap.contains("T78 — Exportaciones del cerebro"));
        assertTrue(roadmap.contains("T79 — Smoke automático del cerebro"));
        assertTrue(roadmap.contains("T80 — Congelación del cerebro V1"));
        assertTrue(roadmap.contains("T81 — Debate y rediseño frontal guiado"));
        assertTrue(roadmap.contains("T82 — Release Candidate"));

        assertTrue(refresh.contains("DocumentSourceImportService"));
        assertTrue(refresh.contains("SourceDocumentRequirementException"));
        assertTrue(report.contains("unsupported(SourceDocumentSnapshot previous"));
        assertTrue(report.contains("detailedSummary()"));
    }
}
