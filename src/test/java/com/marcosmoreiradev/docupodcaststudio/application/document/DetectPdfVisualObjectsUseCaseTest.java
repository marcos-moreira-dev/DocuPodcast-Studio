package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DetectPdfVisualObjectsUseCaseTest {
    @Test
    void clustersNativeTextColumnsIntoOneWholeTableBeforeQwen() {
        PreparedPdfPage page = new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 2, 612, 792,
                PdfPagePreparationStatus.READY, 1,
                List.of(
                        region("REGION", 92, 111, 182, 205, 1,
                                "Región\nTriángulo interior OAB\nSector circular OAB\nTriángulo tangente OAT"),
                        region("AREA-H", 248, 111, 292, 130, 2, "Área"),
                        region("REASON-H", 329, 111, 484, 130, 3, "Razón geométrica"),
                        region("AREA-1", 248, 139, 292, 158, 4, "un medio seno de equis"),
                        region("REASON-1", 329, 139, 484, 158, 5, "Base OA igual a uno"),
                        region("AREA-2", 248, 164, 292, 183, 6, "un medio equis"),
                        region("REASON-2", 329, 164, 484, 205, 7, "Área del sector en radianes"),
                        region("PROSE", 92, 232, 510, 260, 8,
                                "De la comparación de áreas obtenemos lo siguiente.")),
                List.of(), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), "");

        List<PdfVisualObjectProposal> proposals =
                new DetectPdfVisualObjectsUseCase().detect(page);

        PdfVisualObjectProposal table = proposals.stream()
                .filter(value -> value.type() == PdfVisualObjectProposal.Type.TABLE)
                .findFirst().orElseThrow();
        assertEquals(7, table.sourceRegionIds().size());
        assertTrue(table.sourceRegionIds().contains("REGION"));
        assertTrue(table.sourceRegionIds().contains("REASON-2"));
        assertTrue(!table.sourceRegionIds().contains("PROSE"));
        assertTrue(table.geometry().xMinPoints() < 92);
        assertTrue(table.geometry().xMaxPoints() > 484);
    }

    private static PdfRegion region(String id, double x1, double y1,
                                    double x2, double y2, int order,
                                    String text) {
        return new PdfRegion(id, 2, x1, y1, x2, y2, 0, order, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT,
                0.95, "test", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
