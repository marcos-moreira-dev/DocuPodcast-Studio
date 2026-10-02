package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfDerivedTreatmentV3RoundTripTest {
    @TempDir Path temp;

    @Test
    void persistsReviewStatePromptSourceRevisionLanguageAndPageRole() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        String sha = "a".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Libro", "source.pdf", sha,
                1, "pdf-v3", Instant.now(), Instant.now()));
        PdfRegion region = region();
        PdfDerivedTreatment treatment = new PdfDerivedTreatment(
                "DER-1", PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, List.of(region.id()),
                "Una gráfica muestra una curva creciente.", "qwen3-vl:4b-instruct-q8_0",
                "1", 0.91, Instant.now(), PdfDerivedTreatmentState.DRAFT, 7,
                "source-fingerprint", "Describe con tono académico.", Map.of("local", "true"));
        PdfPageAnalysisProfile profile = new PdfPageAnalysisProfile(PdfPageRole.VISUAL_REFERENCE,
                null, new PdfDocumentLanguageProfile("es", 0.92, null),
                PdfPreparationProfile.ENHANCED, 0, List.of("pp-structure-v3", "qwen3-vl"));
        repository.savePage(temp, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 7, List.of(region),
                List.of(treatment), PdfPagePreparationMetrics.empty(), profile, List.of(), ""));

        PreparedPdfPage restored = repository.loadPage(temp, 1).orElseThrow();

        assertEquals(PdfDerivedTreatmentState.DRAFT, restored.derivedTreatments().getFirst().state());
        assertEquals("Describe con tono académico.", restored.derivedTreatments().getFirst().prompt());
        assertEquals(PdfPageRole.VISUAL_REFERENCE, restored.analysisProfile().effectiveRole());
        assertEquals("es", restored.analysisProfile().language().effectiveLanguage());
        assertEquals(PdfPreparationProfile.ENHANCED, restored.analysisProfile().preparationProfile());
    }

    private static PdfRegion region() {
        return new PdfRegion("R1", 1, 40, 100, 500, 500, 0, 0, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.8,
                        "extractor", "parser", "grouping", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 7);
    }
}
