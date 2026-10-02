package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentPresentationPolicyTest {
    @Test
    void pureTextTypesUseTextRender() {
        for (PdfRegionType type : List.of(PdfRegionType.TITLE, PdfRegionType.HEADING,
                PdfRegionType.SUBHEADING, PdfRegionType.PARAGRAPH, PdfRegionType.LIST,
                PdfRegionType.SIDEBAR, PdfRegionType.CAPTION)) {
            PdfRegion region = region(type, Map.of("contentRoute", "OCR_SAFE"));
            assertTrue(DocumentPresentationPolicy.isPureTextPresentationCandidate(region),
                    type.name());
            assertEquals(DocumentPresentationMode.TEXT_RENDER,
                    DocumentPresentationPolicy.forPdf(DocumentContentKind.PROSE,
                            List.of(region), false), type.name());
        }
    }

    @Test
    void visualStructuredAndCodeTypesAlwaysUseSourceCapture() {
        for (PdfRegionType type : List.of(PdfRegionType.MATH, PdfRegionType.TABLE,
                PdfRegionType.IMAGE, PdfRegionType.CODE, PdfRegionType.UNKNOWN)) {
            PdfRegion region = region(type, Map.of("contentRoute", "OCR_SAFE"));
            assertFalse(DocumentPresentationPolicy.isPureTextPresentationCandidate(region),
                    type.name());
            assertEquals(DocumentPresentationMode.SOURCE_CAPTURE,
                    DocumentPresentationPolicy.forPdf(DocumentContentKind.PROSE,
                            List.of(region), false), type.name());
        }
    }

    @Test
    void paragraphWithMathOrAnyVlmRouteUsesWholeSourceCapture() {
        PdfRegion mathParagraph = region(PdfRegionType.PARAGRAPH,
                Map.of("contentRoute", "OCR_SAFE", "mathSourceConvention", "display"));
        assertEquals(DocumentPresentationMode.SOURCE_CAPTURE,
                DocumentPresentationPolicy.forPdf(DocumentContentKind.PROSE,
                        List.of(mathParagraph), false));
        for (String route : List.of("VLM_MIXED", "VLM_VISUAL", "VLM_STRUCTURED")) {
            PdfRegion mixed = region(PdfRegionType.PARAGRAPH,
                    Map.of("contentRoute", route));
            assertEquals(DocumentPresentationMode.SOURCE_CAPTURE,
                    DocumentPresentationPolicy.forPdf(DocumentContentKind.PROSE,
                            List.of(mixed), false), route);
        }
    }

    @Test
    void specializedVisualAndSemanticKindsUseSourceCapture() {
        PdfRegion pure = region(PdfRegionType.CAPTION,
                Map.of("contentRoute", "OCR_SAFE"));
        assertEquals(DocumentPresentationMode.SOURCE_CAPTURE,
                DocumentPresentationPolicy.forPdf(DocumentContentKind.PROSE,
                        List.of(pure), true));
        for (DocumentContentKind kind : List.of(DocumentContentKind.TABLE,
                DocumentContentKind.EQUATION, DocumentContentKind.IMAGE)) {
            assertEquals(DocumentPresentationMode.SOURCE_CAPTURE,
                    DocumentPresentationPolicy.forPdf(kind, List.of(pure), false));
        }
        assertEquals(DocumentPresentationMode.TEXT_RENDER,
                DocumentPresentationPolicy.forPdf(DocumentContentKind.EXTRA,
                        List.of(pure), false));
    }

    @Test
    void absentLegacyRouteKeepsClearlyPureTextCompatible() {
        assertEquals(DocumentPresentationMode.TEXT_RENDER,
                DocumentPresentationPolicy.forPdf(DocumentContentKind.COVER,
                        List.of(region(PdfRegionType.TITLE, Map.of())), false));
    }

    private static PdfRegion region(PdfRegionType type, Map<String, String> attributes) {
        PdfRegionEvidence evidence = new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT,
                0.99, "extractor", "parser", "group", "classifier");
        return new PdfRegion("R-" + type, 1, 20, 20, 300, 100,
                0, 0, "Contenido", type, PdfNarratability.NARRATABLE,
                List.of(), evidence, PdfRegionOverride.empty(), attributes, 1L);
    }
}
