package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UpdatePreparedPdfRegionOverrideUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void updatesFieldsIndependentlyAndCanRestoreAutomaticDecision() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        repository.initialize(tempDir, new PdfDocumentManifest(PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "PDF", "source.pdf", "hash",
                1, "test", now, now));
        repository.savePage(tempDir, page());
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                tempDir, tempDir.resolve("source/source.pdf"), "a".repeat(64));
        UpdatePreparedPdfRegionOverrideUseCase useCase =
                new UpdatePreparedPdfRegionOverrideUseCase(repository);

        useCase.execute(workspace, 1, "region-1",
                new UpdatePreparedPdfRegionOverrideUseCase.OverridePatch(
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.keep(),
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.set(PdfRegionType.HEADING),
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.set(PdfNarratability.NON_NARRATABLE),
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.keep()));

        PdfRegion overridden = repository.loadPage(tempDir, 1).orElseThrow().regions().getFirst();
        assertEquals("Texto automático.", overridden.effectiveText());
        assertEquals(PdfRegionType.HEADING, overridden.effectiveType());
        assertEquals(PdfNarratability.NON_NARRATABLE, overridden.effectiveNarratability());

        useCase.execute(workspace, 1, "region-1",
                UpdatePreparedPdfRegionOverrideUseCase.OverridePatch.restoreAutomaticDecision());

        PdfRegion restored = repository.loadPage(tempDir, 1).orElseThrow().regions().getFirst();
        assertTrue(restored.override().emptyOverride());
        assertEquals(PdfRegionType.PARAGRAPH, restored.effectiveType());
        assertEquals(PdfNarratability.NARRATABLE, restored.effectiveNarratability());
    }

    private static PreparedPdfPage page() {
        PdfRegion region = new PdfRegion("region-1", 1,
                10, 20, 500, 60, 0, 0, "Texto automático.",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE, List.of("prose-shape"),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.9,
                        "test", "test", "test", "test"),
                PdfRegionOverride.empty(), Map.of(), 1);
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(region), List.of(), "");
    }
}
