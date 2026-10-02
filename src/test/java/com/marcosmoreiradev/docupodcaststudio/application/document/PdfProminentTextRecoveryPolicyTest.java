package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfProminentTextRecoveryPolicyTest {
    private static final double WIDTH = 600.0;
    private static final double HEIGHT = 840.0;
    private final PdfRepeatedMarginPolicy margins = new PdfRepeatedMarginPolicy();
    private final PdfUncoveredProminentTextDetector detector =
            new PdfUncoveredProminentTextDetector();

    @Test
    void repeatedTopFurnitureIsNonNarratableButUniqueTitleIsATypeFreeCandidate() {
        PdfTextLayer page2 = layer(2,
                line(2, "Un limite notable, visto de cerca Calculo diferencial",
                        44, 4, 552, 13, 0.99),
                line(2, "La demostracion: encerrar lo desconocido entre dos certezas",
                        44, 38, 470, 53, 0.98),
                line(2, "La idea geometrica permite comparar tres cantidades positivas.",
                        44, 75, 545, 85, 0.98));
        PdfTextLayer page1 = layer(1,
                line(1, "Un limite notable, visto de cerca Calculo diferencial",
                        44, 4, 552, 13, 0.99));
        PdfSemanticPageAnalysis primary = page(
                element("wrong-title", PdfRegionType.TITLE, 0, 0, 980, 35,
                        "Un limite notable, visto de cerca Calculo diferencial"),
                element("body", PdfRegionType.PARAGRAPH, 70, 89, 920, 115,
                        "La idea geometrica permite comparar tres cantidades positivas."));

        List<PdfRepeatedMarginEvidence> repeated = margins.detect(
                page2, List.of(page1));
        PdfSemanticPageAnalysis classified = margins.apply(primary, repeated);
        List<PdfProminentTextCandidate> candidates = detector.detect(
                page2, classified, repeated);

        assertEquals(1, repeated.size());
        assertEquals(PdfRegionType.HEADER, classified.elements().getFirst().type());
        assertEquals(PdfNarratability.NON_NARRATABLE,
                classified.elements().getFirst().narratability());
        assertEquals("false", classified.elements().getFirst()
                .attributes().get("playbackTarget"));
        assertEquals(1, candidates.size());
        assertEquals("La demostracion: encerrar lo desconocido entre dos certezas",
                candidates.getFirst().text());
        assertFalse(candidates.getFirst().signals().isEmpty());
        assertEquals(PdfRegionType.UNKNOWN, detector.recoveryPlan(candidates)
                .rois().getFirst().expectedType());
    }

    @Test
    void positionAloneNeverTurnsAUniqueTopTitleIntoRunningFurniture() {
        PdfTextLayer current = layer(1, line(1,
                "Cuando una fraccion indeterminada revela una certeza",
                44, 10, 470, 24, 0.99));
        PdfTextLayer peer = layer(2, line(2,
                "La demostracion geometrica principal del capitulo",
                44, 10, 470, 24, 0.99));
        PdfSemanticPageAnalysis title = page(element("title", PdfRegionType.TITLE,
                73, 12, 790, 30,
                "Cuando una fraccion indeterminada revela una certeza"));

        List<PdfRepeatedMarginEvidence> repeated = margins.detect(
                current, List.of(peer));

        assertTrue(repeated.isEmpty());
        assertEquals(PdfRegionType.TITLE,
                margins.apply(title, repeated).elements().getFirst().type());
    }

    @Test
    void coveredAndSpecializedRegionsAreNeverProminentRecoveryCandidates() {
        PdfTextLayer visual = layer(1,
                line(1, "Titulo academico ya correctamente localizado",
                        44, 38, 470, 53, 0.99),
                line(1, "Concepto Enero Febrero Marzo Resultado",
                        44, 200, 540, 216, 0.99),
                line(1, "Texto ordinario suficiente para fijar la mediana",
                        44, 90, 540, 100, 0.99),
                line(1, "Otro texto ordinario para fijar la mediana",
                        44, 110, 540, 120, 0.99));
        PdfSemanticPageAnalysis semantic = page(
                element("title", PdfRegionType.TITLE, 70, 43, 790, 64,
                        "Titulo academico ya correctamente localizado"),
                element("table", PdfRegionType.TABLE, 60, 220, 930, 400,
                        "Concepto ; Enero ; Febrero ; Marzo ; Resultado"));

        assertTrue(detector.detect(visual, semantic, List.of()).isEmpty());
    }

    @Test
    void wrongParagraphBoxDoesNotHideAVisuallyProminentDifferentTitle() {
        PdfTextLayer visual = layer(3,
                line(3, "La intuicion visual: una curva que rellena su hueco",
                        44, 39, 470, 54, 0.99),
                line(3, "La funcion no esta definida en cero pero tiene limite.",
                        44, 62, 545, 72, 0.99),
                line(3, "El punto ausente representa un hueco removible.",
                        44, 75, 545, 85, 0.99));
        PdfSemanticPageAnalysis wrong = page(element("wrong-body",
                PdfRegionType.PARAGRAPH, 0, 40, 980, 65,
                "La funcion no esta definida en cero pero tiene limite."));

        List<PdfProminentTextCandidate> candidates = detector.detect(
                visual, wrong, List.of());

        assertEquals(1, candidates.size());
        assertTrue(candidates.getFirst().text().startsWith("La intuicion visual"));
    }

    @Test
    void recoveredHeadingIsMergedOnceAndOrderedByPhysicalGeometry() {
        PdfSemanticPageAnalysis base = page(
                element("header", PdfRegionType.HEADER, 70, 5, 930, 17,
                        "Un limite notable, visto de cerca"),
                element("body", PdfRegionType.PARAGRAPH, 70, 100, 930, 180,
                        "Primer parrafo del desarrollo academico."));
        PdfSemanticPageAnalysis recovered = page(element("recovered", PdfRegionType.HEADING,
                73, 45, 790, 63,
                "La demostracion: encerrar lo desconocido entre dos certezas"));
        PdfSemanticRecoveryMerger merger = new PdfSemanticRecoveryMerger();

        PdfSemanticPageAnalysis once = merger.merge(base, recovered, true);
        PdfSemanticPageAnalysis twice = merger.merge(once, recovered, true);

        assertEquals(3, twice.elements().size());
        assertEquals(List.of(PdfRegionType.HEADER, PdfRegionType.HEADING,
                        PdfRegionType.PARAGRAPH),
                twice.elements().stream().map(
                        PdfSemanticPageAnalysis.Element::type).toList());
    }

    private static PdfTextLayer layer(int page, PdfTextLine... lines) {
        return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(lines), List.of());
    }

    private static PdfTextLine line(int page, String text,
                                    double x1, double y1, double x2, double y2,
                                    double confidence) {
        return new PdfTextLine(page, text,
                new PdfPageRegion(page, x1, y1, x2, y2, WIDTH, HEIGHT),
                List.of(), confidence);
    }

    private static PdfSemanticPageAnalysis page(
            PdfSemanticPageAnalysis.Element... elements) {
        return new PdfSemanticPageAnalysis(1, "es", PdfPageRole.CONTENT,
                List.of(elements), 1.0, List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(
            String id, PdfRegionType type,
            double x1, double y1, double x2, double y2, String text) {
        return new PdfSemanticPageAnalysis.Element(id, 0, type,
                new PdfSemanticPageAnalysis.NormalizedBox(x1, y1, x2, y2),
                text, "", PdfNarratability.NARRATABLE, 0.98,
                List.of(), Map.of("semanticPass", "test"));
    }
}
