package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfBoxNativeTextEvidenceExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cheap no-VLM physical gate for the neighbouring pages of the official PDF. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.headingAudit", matches = "true")
final class PdfAcademicHeadingPhysicalAuditTest {
    @Test
    void pagesOneAndThreeSeparateRunningHeaderFromAcademicTitle() throws Exception {
        Path source = Path.of("D:/Proyectos/Demostracion_limite_notable.pdf");
        Path project = Path.of("D:/Proyectos/Demostracion_limite_notable");
        assertTrue(Files.isRegularFile(source));
        PdfBoxNativeTextEvidenceExtractor extractor =
                new PdfBoxNativeTextEvidenceExtractor();
        List<PdfTextLayer> layers = List.of(extractor.extract(source, 1),
                extractor.extract(source, 2), extractor.extract(source, 3));
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        PdfRepeatedMarginPolicy margins = new PdfRepeatedMarginPolicy();
        PdfUncoveredProminentTextDetector detector =
                new PdfUncoveredProminentTextDetector();

        verifyPage(1, "Cuando una fraccion indeterminada revela una certeza",
                repository.loadPage(project, 1).orElseThrow(), layers,
                margins, detector);
        verifyPage(3, "La intuicion visual",
                repository.loadPage(project, 3).orElseThrow(), layers,
                margins, detector);
    }

    private static void verifyPage(
            int pageNumber, String expectedTitle, PreparedPdfPage canonical,
            List<PdfTextLayer> layers, PdfRepeatedMarginPolicy margins,
            PdfUncoveredProminentTextDetector detector) {
        PdfTextLayer current = layers.get(pageNumber - 1);
        List<PdfTextLayer> peers = layers.stream()
                .filter(layer -> layer.pageNumber() != pageNumber).toList();
        List<PdfRepeatedMarginEvidence> repeated = margins.detect(current, peers);
        assertFalse(repeated.isEmpty(), "Debe existir evidencia multipagina de margen");
        PdfSemanticPageAnalysis classified = margins.apply(
                semantic(canonical), repeated);
        String expected = PdfSemanticCoverageValidator.normalized(expectedTitle);
        boolean alreadyCanonical = classified.elements().stream()
                .anyMatch(element -> PdfSemanticCoverageValidator.normalized(
                        element.sourceText()).contains(expected));
        boolean recoverable = detector.detect(current, classified, repeated).stream()
                .anyMatch(candidate -> PdfSemanticCoverageValidator.normalized(
                        candidate.text()).contains(expected));
        assertTrue(alreadyCanonical || recoverable,
                "El titulo debe estar canonico o justificadamente recuperable en P"
                        + pageNumber);
        assertTrue(classified.elements().stream()
                .filter(element -> repeated.stream().anyMatch(value ->
                        PdfRepeatedMarginPolicy.textSimilarity(
                                element.sourceText(), value.text()) >= 0.78))
                .allMatch(element -> element.narratability()
                        == PdfNarratability.NON_NARRATABLE));
    }

    private static PdfSemanticPageAnalysis semantic(PreparedPdfPage page) {
        List<PdfSemanticPageAnalysis.Element> elements = page.regions().stream()
                .map(region -> element(page, region)).toList();
        return new PdfSemanticPageAnalysis(page.pageNumber(),
                page.analysisProfile().language().effectiveLanguage(),
                page.analysisProfile().effectiveRole() == null
                        ? PdfPageRole.UNKNOWN : page.analysisProfile().effectiveRole(),
                elements, 1.0, List.of());
    }

    private static PdfSemanticPageAnalysis.Element element(
            PreparedPdfPage page, PdfRegion region) {
        return new PdfSemanticPageAnalysis.Element(region.id(),
                region.readingOrder(), region.effectiveType(),
                new PdfSemanticPageAnalysis.NormalizedBox(
                        region.xMin() / page.widthPoints() * 1000.0,
                        region.yMin() / page.heightPoints() * 1000.0,
                        region.xMax() / page.widthPoints() * 1000.0,
                        region.yMax() / page.heightPoints() * 1000.0),
                region.effectiveText(), "", region.effectiveNarratability(),
                region.evidence().confidence(), region.reasons(), region.attributes());
    }
}
