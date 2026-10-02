package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationMetrics;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonPreparedPdfDocumentRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void storesManifestAndPagesAsValidatedUtf8WithoutTemporaryResidue() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        repository.initialize(tempDir, manifest("áéíóú-ñ.pdf", "hash-uno", 42));
        repository.savePage(tempDir, page(42, "La página contiene ñ, tildes y signos ¿correctos?"));

        assertEquals(42, repository.loadManifest(tempDir).orElseThrow().pageCount());
        PreparedPdfPage loaded = repository.loadPage(tempDir, 42).orElseThrow();
        assertEquals("La página contiene ñ, tildes y signos ¿correctos?",
                loaded.regions().getFirst().effectiveText());
        assertEquals(PdfNarratability.NARRATABLE,
                loaded.regions().getFirst().effectiveNarratability());
        assertEquals(1250, loaded.preparationMetrics().elapsedMillis());
        assertEquals(430, loaded.preparationMetrics().cpuMillis());
        assertTrue(loaded.preparationMetrics().ocrUsed());

        Path pageFile = tempDir.resolve("document/pages/page-000042.json");
        String json = Files.readString(pageFile, StandardCharsets.UTF_8);
        assertTrue(json.contains("página contiene ñ"));
        assertFalse(Files.exists(pageFile.resolveSibling("page-000042.json.tmp")));
        assertFalse(Files.exists(tempDir.resolve("document/manifest.json.tmp")));
    }

    @Test
    void sourceChangeClearsOnlyPreparedPdfPages() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        repository.initialize(tempDir, manifest("origen.pdf", "hash-uno", 2));
        repository.savePage(tempDir, page(1, "Texto anterior suficientemente narrable."));
        Path unrelated = tempDir.resolve("document/pages/nota-del-usuario.txt");
        Files.writeString(unrelated, "conservar", StandardCharsets.UTF_8);

        repository.initialize(tempDir, manifest("nuevo.pdf", "hash-dos", 2));

        assertTrue(repository.loadPages(tempDir).isEmpty());
        assertTrue(Files.isRegularFile(unrelated));
    }

    @Test
    void rejectsCorruptManifestInsteadOfFallingBackToAnotherPdfStore() throws Exception {
        Path manifest = tempDir.resolve("document/manifest.json");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, "{\"schemaVersion\": 2,", StandardCharsets.UTF_8);

        assertThrows(IOException.class,
                () -> new JsonPreparedPdfDocumentRepository().loadManifest(tempDir));
    }

    @Test
    void persistsRegenerableDerivativesWithoutReplacingOriginalRegionText() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        repository.initialize(tempDir, manifest("tablas.pdf", "hash-tablas", 1));
        PreparedPdfPage original = page(1, "A | B | C");
        PdfDerivedTreatment derived = new PdfDerivedTreatment("DER-1",
                PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION,
                List.of("R-1"), "La tabla contiene tres columnas.", "table-reader-local",
                "1.0", 0.88, Instant.parse("2026-07-28T01:00:00Z"), Map.of("local", "true"));
        repository.savePage(tempDir, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 2, original.regions(), List.of(derived), List.of(), ""));

        PreparedPdfPage loaded = repository.loadPage(tempDir, 1).orElseThrow();

        assertEquals("A | B | C", loaded.regions().getFirst().text());
        assertEquals("La tabla contiene tres columnas.", loaded.derivedTreatments().getFirst().derivedText());
    }

    @Test
    void persistsSemanticVlmEvidenceWithoutIntroducingAnotherPdfStore()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        repository.initialize(tempDir, manifest("semantic.pdf", "hash-semantic", 1));
        PdfRegion semantic = new PdfRegion("SEM-1", 1,
                20, 30, 590, 180, 0, 0, "Parrafo leido visualmente.",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC,
                0.91, "qwen-test", "page-elements-v1",
                "page-semantic-v1", "page-human-reading-v1"),
                PdfRegionOverride.empty(),
                Map.of("bboxCoordinateSpace", "NORMALIZED_0_1000"), 1);
        repository.savePage(tempDir, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(semantic),
                List.of(), List.of(), ""));

        PreparedPdfPage reopened = repository.loadPage(tempDir, 1).orElseThrow();

        assertEquals(PdfRegionOrigin.VLM_SEMANTIC,
                reopened.regions().getFirst().evidence().origin());
        assertEquals("NORMALIZED_0_1000", reopened.regions().getFirst()
                .attributes().get("bboxCoordinateSpace"));
    }

    @Test
    void invalidatesVolatileRegionIndexWheneverAPageIsPublished() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        repository.initialize(tempDir, manifest("indice.pdf", "hash-indice", 1));
        repository.savePage(tempDir, page(1, "Texto anterior."));

        var firstIndex = repository.loadRegionIndex(tempDir);
        assertEquals("Texto anterior.", firstIndex.regions().getFirst().region().effectiveText());

        repository.savePage(tempDir, page(1, "Texto actualizado."));

        var refreshedIndex = repository.loadRegionIndex(tempDir);
        assertEquals("Texto actualizado.",
                refreshedIndex.regions().getFirst().region().effectiveText());
    }

    private static PdfDocumentManifest manifest(String sourceFile, String hash, int pages) {
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        return new PdfDocumentManifest(PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Álgebra y narración", sourceFile, hash, pages,
                "test-v3", now, now);
    }

    private static PreparedPdfPage page(int pageNumber, String text) {
        PdfRegion region = new PdfRegion("R-" + pageNumber, pageNumber,
                10, 20, 500, 60, 0, 0, text,
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of("prose-shape"),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.94,
                        "tesseract-test", "tsv-test", "group-test", "classifier-test"),
                new PdfRegionOverride(null, null, PdfNarratability.NARRATABLE, null),
                Map.of("ruta", "página-" + pageNumber), 1);
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, pageNumber, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(region), List.of(),
                new PdfPagePreparationMetrics(1250, 430, 96_000_000, false, true),
                List.of(), "");
    }
}
