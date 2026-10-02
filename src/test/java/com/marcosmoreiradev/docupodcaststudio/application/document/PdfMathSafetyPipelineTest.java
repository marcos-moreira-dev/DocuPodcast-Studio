package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.LocalResourceScheduler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class PdfMathSafetyPipelineTest {
    @Test
    void groupsFragmentsAndDistinguishesInlineFromBlockFormula() {
        PreparedPdfPage page = page(List.of(
                region("PROSE", 10, 10, 180, 28, PdfRegionType.PARAGRAPH),
                region("M1", 190, 11, 220, 28, PdfRegionType.MATH),
                region("M2", 222, 11, 250, 28, PdfRegionType.MATH),
                region("M3", 80, 100, 530, 145, PdfRegionType.MATH)));

        List<PdfFormulaUnit> units =
                new DetectPdfFormulaUnitsUseCase().detect(page);

        assertEquals(2, units.size());
        assertEquals(PdfFormulaKind.INLINE_FORMULA, units.getFirst().kind());
        assertEquals(List.of("M1", "M2"), units.getFirst().sourceRegionIds());
        assertEquals(PdfFormulaKind.BLOCK_FORMULA, units.getLast().kind());
    }

    @Test
    void noEngineFallbackSuppressesRawOcrAndAlwaysRemainsDraft()
            throws Exception {
        PdfRegion formula = region("MATH-RAW", 80, 100, 530, 145,
                PdfRegionType.MATH);
        PreparedPdfPage page = page(List.of(formula));
        MediaCapabilityService media = new MediaCapabilityService(
                MediaEnginePlatform.empty(),
                LocalResourceScheduler.safeDefaults());
        TransversalMathPdfTreatmentEngine engine =
                new TransversalMathPdfTreatmentEngine(media);

        PdfDerivedTreatment result = engine.generate(page, List.of(formula),
                request(formula, Map.of("fallbackPolicy", "REQUIRE_REVIEW")));

        assertEquals(PdfDerivedTreatmentState.DRAFT, result.state());
        assertEquals("true", result.metadata().get("rawOcrSuppressed"));
        assertFalse(result.derivedText().contains(formula.text()));
        assertEquals("REQUIRE_REVIEW",
                result.metadata().get("fallbackPolicy"));
    }

    @Test
    void rejectsUnsafeMathMlAndMeasuresSymbolError() {
        assertThrows(IllegalArgumentException.class, () ->
                MathMlSafetyValidator.validate(
                        "<!DOCTYPE math [<!ENTITY x SYSTEM 'file:///x'>]><math>&x;</math>"));
        assertThrows(IllegalArgumentException.class, () ->
                MathMlSafetyValidator.validate("<html/>"));
        assertEquals(0.0,
                TransversalMathPdfTreatmentEngine.normalizedEditDistance(
                        "x + 2", "x+2"));
        assertTrue(TransversalMathPdfTreatmentEngine.normalizedEditDistance(
                "x+2", "x-2") > 0);
    }

    private static PdfDerivedTreatmentGenerationRequest request(
            PdfRegion region, Map<String, String> options) {
        return new PdfDerivedTreatmentGenerationRequest(
                Path.of(".").toAbsolutePath(), 1,
                PdfDerivedTreatmentKind.MATHEMATICAL_READING,
                List.of(region.id()), TransversalMathPdfTreatmentEngine.ID,
                true, options);
    }

    private static PreparedPdfPage page(List<PdfRegion> regions) {
        return new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, regions, List.of(), "");
    }

    private static PdfRegion region(String id, double xMin, double yMin,
                                    double xMax, double yMax,
                                    PdfRegionType type) {
        return new PdfRegion(id, 1, xMin, yMin, xMax, yMax,
                (int) yMin, 0, id, type,
                type == PdfRegionType.PARAGRAPH
                        ? PdfNarratability.NARRATABLE
                        : PdfNarratability.NON_NARRATABLE,
                List.of(), new PdfRegionEvidence(
                PdfRegionOrigin.OCR_LOCAL, 0.8, "ocr", "parser", "group", "class"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
