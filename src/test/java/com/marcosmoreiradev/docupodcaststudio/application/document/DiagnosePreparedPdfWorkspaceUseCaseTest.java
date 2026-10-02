package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiagnosePreparedPdfWorkspaceUseCaseTest {
    @TempDir
    Path root;

    @Test
    void reportsPreparationTimeCpuHeapStorageAndOcrRate() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Diagnóstico", "origen.pdf",
                "a".repeat(64), 2, "test", now, now));
        repository.savePage(root, page(1, PdfRegionOrigin.NATIVE_TEXT,
                new PdfPagePreparationMetrics(100, 40, 80_000_000, true, false)));
        repository.savePage(root, page(2, PdfRegionOrigin.OCR_LOCAL,
                new PdfPagePreparationMetrics(300, 120, 140_000_000, false, true)));
        Path cache = root.resolve(".docupodcast-cache/pdf-ocr");
        Files.createDirectories(cache);
        Files.writeString(cache.resolve("page.cache"), "cache");

        PdfWorkspaceDiagnosticReport report =
                new DiagnosePreparedPdfWorkspaceUseCase(repository).diagnose(root, cache);

        assertTrue(report.manifestValid());
        assertEquals(2, report.preparedPages());
        assertEquals(400, report.totalPreparationMillis());
        assertEquals(160, report.totalCpuMillis());
        assertEquals(140_000_000, report.maximumObservedHeapBytes());
        assertEquals(200.0, report.averagePreparationMillis());
        assertEquals(0.5, report.ocrRegionRate());
        assertTrue(report.preparedBytes() > 0);
        assertTrue(report.cacheBytes() > 0);
    }

    private static PreparedPdfPage page(int number,
                                        PdfRegionOrigin origin,
                                        PdfPagePreparationMetrics metrics) {
        PdfRegion region = new PdfRegion("R-" + number, number, 10, 10, 500, 60,
                0, 0, "Texto de prueba " + number,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(origin, 0.9, "extractor", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, number, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(region), List.of(),
                metrics, List.of(), "");
    }
}
