package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfDocumentLayoutAnalyzerTest {
    @Test
    void preservesRepeatedFurnitureAndOrdersColumns() {
        List<PreparedPdfPage> pages = new ArrayList<>();
        for (int page = 1; page <= 5; page++) {
            pages.add(new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                    page, 600, 800, PdfPagePreparationStatus.READY, 1,
                    List.of(region("header-" + page, page, 10, 10, 590, 30, "Manual de vuelo"),
                            region("left-" + page, page, 20, 100, 270, 150, "Texto de la columna izquierda."),
                            region("right-" + page, page, 330, 90, 580, 140, "Texto de la columna derecha."),
                            region("footer-" + page, page, 10, 770, 590, 790, "Edición de prueba")),
                    List.of(), ""));
        }
        PdfDocumentManifest manifest = new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Manual", "manual.pdf",
                "a".repeat(64), 5, "test", Instant.EPOCH, Instant.EPOCH);

        PdfLayoutAnalysisResult result = new PdfDocumentLayoutAnalyzer().analyze(manifest, pages);

        assertEquals(PdfRegionType.HEADER, result.pages().getFirst().regions().getFirst().automaticType());
        assertTrue(result.pages().getFirst().regions().stream()
                .anyMatch(region -> region.automaticType() == PdfRegionType.FOOTER
                        && region.automaticNarratability() == PdfNarratability.NON_NARRATABLE));
        assertTrue(result.manifest().analysisSummary().maximumColumnCount() >= 2);
        assertEquals(5, result.manifest().analysisSummary().observedPages());
    }

    @Test
    void sparseDiagramLabelsRemainReviewableAndDoNotJumpAheadOfProse() {
        PreparedPdfPage page = new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 600, 800, PdfPagePreparationStatus.READY, 1,
                List.of(
                        region("header", 1, 20, 10, 580, 28,
                                "Manual de cálculo"),
                        region("body", 1, 20, 90, 580, 180,
                                "La explicación principal conserva su orden natural."),
                        region("label", 1, 430, 260, 500, 275,
                                "tangente en A"),
                        region("caption", 1, 20, 370, 580, 390,
                                "Figura 1. Construcción geométrica."),
                        region("after", 1, 20, 410, 580, 470,
                                "La conclusión continúa después de la figura.")),
                List.of(), "");
        PdfDocumentManifest manifest = new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Manual",
                "manual.pdf", "a".repeat(64), 1, "test",
                Instant.EPOCH, Instant.EPOCH);

        PreparedPdfPage analyzed = new PdfDocumentLayoutAnalyzer()
                .analyze(manifest, List.of(page)).pages().getFirst();

        assertEquals(List.of("header", "body", "label", "caption", "after"),
                analyzed.regions().stream().map(PdfRegion::id).toList());
        PdfRegion label = analyzed.regions().stream()
                .filter(region -> region.id().equals("label"))
                .findFirst().orElseThrow();
        assertEquals(PdfRegionType.UNKNOWN, label.automaticType());
        assertEquals(PdfNarratability.UNCERTAIN,
                label.automaticNarratability());
    }

    private static PdfRegion region(String id, int page, double x1, double y1, double x2, double y2, String text) {
        return new PdfRegion(id, page, x1, y1, x2, y2, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 1.0,
                        "native", "bbox", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
