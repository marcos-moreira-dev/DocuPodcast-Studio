package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfSemanticRecoveryPlannerTest {
    private final PdfSemanticRecoveryPlanner planner =
            new PdfSemanticRecoveryPlanner();

    @Test
    void easyAcceptedPageCreatesNoRecoveryWork() {
        PdfSemanticRecoveryPlan plan = planner.plan(page(element(
                        PdfRegionType.PARAGRAPH, 80, 100, 920, 300,
                        "Texto completo de control.")),
                new PdfSemanticCoverageResult(PdfSemanticCoverageStatus.ACCEPTED,
                        true, 1.0, List.of(), List.of()),
                layer(), reliable());

        assertTrue(plan.empty());
    }

    @Test
    void denseTableUsesItsExistingBoxAndReason() {
        PdfSemanticRecoveryPlan plan = planner.plan(page(element(
                        PdfRegionType.TABLE, 90, 180, 910, 700,
                        "Resumen de la tabla")),
                new PdfSemanticCoverageResult(PdfSemanticCoverageStatus.REJECTED,
                        true, 0.4, List.of("tableMissingVisibleCells"),
                        List.of("Ventas 1250 1430")), layer(), reliable());

        assertEquals(1, plan.rois().size());
        assertEquals(PdfSemanticRecoveryReason.DENSE_TABLE_INCOMPLETE,
                plan.rois().getFirst().reason());
        assertEquals(PdfRegionType.TABLE, plan.rois().getFirst().expectedType());
        assertTrue(plan.rois().getFirst().box().xMin() < 90);
    }

    @Test
    void missingRightColumnIsLocalizedFromReliablePdfBoxEvidence() {
        PdfPageRegion right = new PdfPageRegion(1, 330, 120, 560, 160,
                600, 800);
        PdfTextLayer evidence = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(new PdfTextLine(1,
                        "Párrafo omitido de la columna derecha", right,
                        List.of(new PdfTextToken("Párrafo", right, 1.0)), 1.0)),
                List.of());
        PdfSemanticRecoveryPlan plan = planner.plan(page(element(
                        PdfRegionType.PARAGRAPH, 60, 100, 450, 300,
                        "Columna izquierda presente")),
                new PdfSemanticCoverageResult(PdfSemanticCoverageStatus.REJECTED,
                        true, 0.5, List.of("nativeTextCoverageGap"),
                        List.of("Párrafo omitido de la columna derecha")),
                evidence, reliable());

        assertEquals(1, plan.rois().size());
        assertEquals(PdfSemanticRecoveryReason.MULTICOLUMN_GAP_OR_ORDER,
                plan.rois().getFirst().reason());
        assertTrue(plan.rois().getFirst().box().xMin() > 500);
    }

    @Test
    void unreliablePdfBoxDoesNotInventTextGapRoi() {
        PdfSemanticRecoveryPlan plan = planner.plan(page(element(
                        PdfRegionType.PARAGRAPH, 50, 80, 950, 300, "Texto")),
                new PdfSemanticCoverageResult(PdfSemanticCoverageStatus.REJECTED,
                        false, 0.0, List.of("nativeTextCoverageGap"),
                        List.of("texto dudoso")), layer(),
                new PdfNativeTextQualityReport(PdfNativeTextQuality.SUSPECT,
                        0.4, List.of()));

        assertTrue(plan.empty());
    }

    @Test
    void singleLetterFormulaFragmentsDoNotBecomeMicroscopicRecoveryRois() {
        PdfPageRegion sentence = new PdfPageRegion(1, 40, 70, 550, 90,
                600, 800);
        PdfPageRegion symbolB = new PdfPageRegion(1, 490, 72, 500, 88,
                600, 800);
        PdfPageRegion nextSentence = new PdfPageRegion(1, 40, 95, 550, 115,
                600, 800);
        PdfTextLayer evidence = new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(
                        textLine("La altura del punto es seno de x y completa la primera explicacion.", sentence),
                        textLine("B", symbolB),
                        textLine("La longitud del arco y la tangente completan la comparacion.", nextSentence)),
                List.of());
        PdfSemanticRecoveryPlan plan = planner.plan(page(element(
                        PdfRegionType.TITLE, 50, 20, 950, 60, "Titulo")),
                new PdfSemanticCoverageResult(PdfSemanticCoverageStatus.REJECTED,
                        true, 0.1, List.of("nativeTextCoverageGap"),
                        List.of("La altura del punto es seno de x y completa la primera explicacion.",
                                "B",
                                "La longitud del arco y la tangente completan la comparacion.")),
                evidence, reliable());

        assertEquals(1, plan.rois().size());
        assertTrue(plan.rois().getFirst().box().xMin() < 100);
        assertTrue(plan.rois().getFirst().box().xMax() > 900);
        assertTrue(plan.rois().getFirst().missingEvidence().stream()
                .noneMatch("B"::equals));
    }

    private static PdfTextLine textLine(String text, PdfPageRegion box) {
        return new PdfTextLine(1, text, box,
                List.of(new PdfTextToken(text, box, 1.0)), 1.0);
    }

    private static PdfNativeTextQualityReport reliable() {
        return new PdfNativeTextQualityReport(PdfNativeTextQuality.RELIABLE,
                1.0, List.of());
    }

    private static PdfTextLayer layer() {
        return new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(), List.of());
    }

    private static PdfSemanticPageAnalysis page(
            PdfSemanticPageAnalysis.Element... elements) {
        return new PdfSemanticPageAnalysis(1, "es", PdfPageRole.CONTENT,
                List.of(elements), 1.0, List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(
            PdfRegionType type, double x1, double y1, double x2, double y2,
            String source) {
        return new PdfSemanticPageAnalysis.Element("", 0, type,
                new PdfSemanticPageAnalysis.NormalizedBox(x1, y1, x2, y2),
                source, "", PdfNarratability.NARRATABLE, 1.0,
                List.of(), Map.of());
    }
}
