package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PdfReadingOrderResolverTest {

    @Test
    void mixedWidthSingleColumnFollowsVisualOrderInsteadOfPuttingWideBodyFirst() {
        PreparedPdfPage page = page(List.of(
                region("BODY", 40, 90, 560, 220,
                        "La pregunta. Explicación principal completa."),
                region("HEADER", 40, 5, 270, 20, "Un límite notable"),
                region("TITLE", 40, 40, 430, 65,
                        "Cuando una fracción revela una certeza"),
                region("LABEL-A", 410, 250, 470, 265, "tangente en A"),
                region("LABEL-B", 100, 270, 180, 285, "sector circular"),
                region("CAPTION", 40, 373, 560, 390,
                        "Figura 1. Construcción geométrica."),
                region("AFTER", 40, 399, 560, 450,
                        "Esta conclusión explica el resultado.")));

        List<String> ids = new PdfReadingOrderResolver().resolve(page).stream()
                .map(PdfRegion::id)
                .toList();

        assertEquals(List.of(
                "HEADER", "TITLE", "BODY", "LABEL-A", "LABEL-B",
                "CAPTION", "AFTER"), ids);
    }

    @Test
    void genuineColumnsAreReadTopToBottomBeforeMovingRight() {
        PreparedPdfPage page = page(List.of(
                region("HEADER", 20, 10, 580, 30, "Manual académico"),
                region("RIGHT-1", 330, 90, 580, 140,
                        "Primer párrafo de la columna derecha."),
                region("LEFT-1", 20, 100, 270, 150,
                        "Primer párrafo de la columna izquierda."),
                region("RIGHT-2", 330, 190, 580, 240,
                        "Segundo párrafo de la columna derecha."),
                region("LEFT-2", 20, 200, 270, 250,
                        "Segundo párrafo de la columna izquierda."),
                region("FOOTER", 20, 770, 580, 790, "Fin de página")));

        List<String> ids = new PdfReadingOrderResolver().resolve(page).stream()
                .map(PdfRegion::id)
                .toList();

        assertEquals(List.of(
                "HEADER", "LEFT-1", "LEFT-2",
                "RIGHT-1", "RIGHT-2", "FOOTER"), ids);
    }

    private static PreparedPdfPage page(List<PdfRegion> regions) {
        return new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 600, 800, PdfPagePreparationStatus.READY, 1,
                regions, List.of(), "");
    }

    private static PdfRegion region(String id, double x1, double y1,
                                    double x2, double y2, String text) {
        return new PdfRegion(
                id, 1, x1, y1, x2, y2, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of("fixture"),
                new PdfRegionEvidence(
                        PdfRegionOrigin.NATIVE_TEXT, 0.99,
                        "fixture", "fixture", "fixture", "fixture"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
