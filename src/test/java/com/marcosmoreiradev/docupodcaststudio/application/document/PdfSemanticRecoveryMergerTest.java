package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfSemanticRecoveryMergerTest {
    private final PdfSemanticRecoveryMerger merger =
            new PdfSemanticRecoveryMerger();

    @Test
    void completeRecoveredTableReplacesPartialTableWithoutDuplication() {
        var partial = element("primary-table", 0, PdfRegionType.TABLE,
                100, 200, 900, 600, "Resumen de ventas");
        var complete = element("recovery-table", 0, PdfRegionType.TABLE,
                90, 190, 910, 610,
                "Concepto ; Enero\nVentas ; 1250\nCostos ; 710");

        PdfSemanticPageAnalysis result = merger.merge(page(partial),
                page(complete), true);

        assertEquals(1, result.reportedPageNumber());
        assertEquals(1.0, result.confidence());
        assertEquals(1, result.elements().size());
        assertTrue(result.elements().getFirst().sourceText().contains("Costos ; 710"));
        assertEquals("primary-table", result.elements().getFirst().responseId());
        assertEquals("primary-table", result.elements().getFirst()
                .attributes().get("replacedResponseId"));
        assertEquals("recovery-table", result.elements().getFirst()
                .attributes().get("recoveryResponseId"));
    }

    @Test
    void similarWordsInDistinctBoxesAreNotDeduplicated() {
        var left = element("left", 0, PdfRegionType.PARAGRAPH,
                50, 100, 450, 250, "Resultados importantes del estudio");
        var right = element("right", 0, PdfRegionType.PARAGRAPH,
                550, 100, 950, 250, "Resultados importantes del segundo estudio");

        PdfSemanticPageAnalysis result = merger.merge(page(left), page(right), true);

        assertEquals(2, result.elements().size());
    }

    @Test
    void twoColumnsUseColumnThenVerticalOrderNotCompletionOrder() {
        var leftTop = element("l1", 9, PdfRegionType.PARAGRAPH,
                50, 100, 450, 220, "Izquierda uno");
        var leftBottom = element("l2", 8, PdfRegionType.PARAGRAPH,
                50, 300, 450, 420, "Izquierda dos");
        var rightTop = element("r1", 0, PdfRegionType.PARAGRAPH,
                550, 90, 950, 210, "Derecha uno");

        PdfSemanticPageAnalysis result = merger.merge(
                page(leftTop, leftBottom), page(rightTop), true);

        assertEquals(List.of("Izquierda uno", "Izquierda dos", "Derecha uno"),
                result.elements().stream().map(
                        PdfSemanticPageAnalysis.Element::sourceText).toList());
        assertEquals("1", result.elements().getLast().attributes()
                .get("columnIndex"));
    }

    @Test
    void imageRecoveryWithLabelsButNoSpeechDoesNotInventAnExplanation() {
        var primary = element("primary-image", 0, PdfRegionType.IMAGE,
                200, 80, 750, 340, "Figura: curva principal");
        var labelsOnly = element("recovery-image", 0, PdfRegionType.IMAGE,
                186, 66, 764, 354,
                "y = sin(x)/x\nhueco\n-pi\npi\nx");

        PdfSemanticPageAnalysis result = merger.merge(page(primary),
                page(labelsOnly), true);

        assertEquals(1, result.elements().size());
        assertTrue(result.elements().getFirst().sourceText().contains("hueco"));
        assertTrue(result.elements().getFirst().narrationText().isBlank());
    }

    @Test
    void verifierMayEnrichContentButCannotReplacePrimaryGeometry() {
        var primary = element("primary-image", 5, PdfRegionType.IMAGE,
                300, 275, 650, 435, "curva y = sin(x)/x");
        var verifier = new PdfSemanticPageAnalysis.Element("verifier-image", 0,
                PdfRegionType.IMAGE,
                new PdfSemanticPageAnalysis.NormalizedBox(0, 0, 1000, 1000),
                "curva y = sin(x)/x", "La curva converge a uno.",
                PdfNarratability.NARRATABLE, 1.0, List.of(),
                Map.of("semanticPass", "verifier"));

        PdfSemanticPageAnalysis result = merger.merge(page(primary), page(verifier), false);

        var accepted = result.elements().getFirst();
        assertEquals(primary.box(), accepted.box());
        assertEquals("La curva converge a uno.", accepted.narrationText());
        assertEquals("test", accepted.attributes().get("layoutAuthority"));
        assertEquals("verifier", accepted.attributes().get("contentAuthority"));
    }

    @Test
    void refinementKeepsParentAsContainerAndPublishesOnlyItsLeaves() {
        var parent = element("parent", 0, PdfRegionType.PARAGRAPH,
                20, 20, 980, 900, "pagina completa");
        var title = element("title", 9, PdfRegionType.TITLE,
                80, 60, 920, 120, "Titulo");
        var paragraph = element("paragraph", 1, PdfRegionType.PARAGRAPH,
                80, 150, 920, 300, "Parrafo independiente");

        PdfSemanticPageAnalysis result = merger.replacePlaybackParentWithChildren(
                page(parent), parent, page(paragraph, title));

        assertEquals(3, result.elements().size());
        var structuralParent = result.elements().stream()
                .filter(value -> value.responseId().equals("parent")).findFirst().orElseThrow();
        assertEquals("CONTAINER", structuralParent.attributes().get("regionRole"));
        assertEquals("false", structuralParent.attributes().get("playbackTarget"));
        assertTrue(structuralParent.sourceText().isBlank());
        assertEquals(List.of("title", "paragraph"), result.elements().stream()
                .filter(value -> "LEAF".equals(value.attributes().get("regionRole")))
                .map(PdfSemanticPageAnalysis.Element::responseId).toList());
        assertTrue(result.elements().stream()
                .filter(value -> "LEAF".equals(value.attributes().get("regionRole")))
                .allMatch(value -> "parent".equals(
                        value.attributes().get("parentLayoutNodeId"))));
    }

    private static PdfSemanticPageAnalysis page(
            PdfSemanticPageAnalysis.Element... elements) {
        return new PdfSemanticPageAnalysis(1, "es", PdfPageRole.CONTENT,
                List.of(elements), 1.0, List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(
            String id, int order, PdfRegionType type,
            double x1, double y1, double x2, double y2, String source) {
        return new PdfSemanticPageAnalysis.Element(id, order, type,
                new PdfSemanticPageAnalysis.NormalizedBox(x1, y1, x2, y2),
                source, "", PdfNarratability.NARRATABLE, 1.0,
                List.of(), Map.of("semanticPass", "test"));
    }
}
