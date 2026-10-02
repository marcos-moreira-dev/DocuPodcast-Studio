package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializedImportedDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfPageMapUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class ReadableDocumentWorkspaceRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void materializesSourceAndDocumentJsonUsingRelativeAssetsAndDiagnostics() throws Exception {
        Path source = tempDir.resolve("notas.docx");
        Files.writeString(source, "fake docx bytes");
        ReadableDocument document = new ReadableDocument(
                "Notas",
                SourceDocumentFormat.DOCX,
                source,
                List.of(DocumentBlock.of("B0001", DocumentBlockType.PARAGRAPH, "Hola mundo", "", Map.of("styleId", "Normal"))),
                new DocumentImportReport(List.of(DocumentImportIssue.warning("DOCX_NO_HEADINGS", "Sin títulos detectados")))
        );
        Path projectFile = tempDir.resolve("Proyecto.docupodcast.json");

        ReadableDocumentWorkspaceRepository repository = new ReadableDocumentWorkspaceRepository();
        MaterializedImportedDocument materialized = repository.materialize(document, ReadingProfile.academicDefaults(), projectFile);

        assertEquals(ProjectAssetKind.SOURCE_DOCUMENT, materialized.sourceDocumentAsset().kind());
        assertEquals("source/notas.docx", materialized.sourceDocumentAsset().relativePath());
        assertEquals(ProjectAssetKind.IMPORTED_DOCUMENT, materialized.importedDocumentAsset().kind());
        assertEquals("document/document.json", materialized.importedDocumentAsset().relativePath());
        assertTrue(Files.exists(tempDir.resolve("source/notas.docx")));
        BlockDocumentSource blockSource = (BlockDocumentSource) materialized.projectSource();
        assertEquals(tempDir.resolve("source/notas.docx").toAbsolutePath().normalize(),
                blockSource.sourcePath());
        String json = Files.readString(tempDir.resolve("document/document.json"));
        assertTrue(json.contains("Hola mundo"));
        assertTrue(json.contains("importReport"));
        assertTrue(json.contains("DOCX_NO_HEADINGS"));
        assertTrue(json.contains("metadata"));
        assertTrue(json.contains("readingProfile"));
        assertTrue(json.contains("Documento académico Word"));
    }

    @Test
    void materializingPdfCreatesOnlyManifestAndNoPreparedPages() throws Exception {
        Path source = tempDir.resolve("manual-ñ.pdf");
        Files.writeString(source, "%PDF-test");
        Path staging = tempDir.resolve("staging");
        JsonPreparedPdfDocumentRepository pdfRepository = new JsonPreparedPdfDocumentRepository();
        pdfRepository.initialize(staging, manifest("Manual con tildes", source, 3));
        PreparedPdfSource document = new PreparedPdfSource(
                new PreparedPdfWorkspaceRef(staging, source, sha256(source)), "Manual con tildes");
        ReadableDocumentWorkspaceRepository repository =
                new ReadableDocumentWorkspaceRepository(pdfRepository);
        MaterializedImportedDocument materialized = repository.materialize(
                document, ReadingProfile.academicDefaults(), tempDir.resolve("Proyecto.docupodcast.json"));

        assertEquals("document/manifest.json", materialized.importedDocumentAsset().relativePath());
        assertTrue(Files.isRegularFile(tempDir.resolve("document/manifest.json")));
        assertFalse(Files.exists(tempDir.resolve("document/document.json")));
        assertFalse(Files.isDirectory(tempDir.resolve("document/pages")));
        PreparedPdfSource pdfSource = (PreparedPdfSource) materialized.projectSource();
        assertEquals(3, new JsonPreparedPdfDocumentRepository()
                .loadManifest(pdfSource.workspace().projectRoot()).orElseThrow().pageCount());
    }

    @Test
    void preliminaryPdfSnapshotRequiresReimportInsteadOfSilentMigration() throws Exception {
        Path documentJson = tempDir.resolve("document/document.json");
        Files.createDirectories(documentJson.getParent());
        Files.writeString(documentJson, """
                {
                  "title": "PDF viejo",
                  "format": "PDF",
                  "sourcePath": "viejo.pdf",
                  "blocks": [],
                  "importReport": {"issues": []}
                }
                """);

        java.io.IOException error = assertThrows(java.io.IOException.class,
                () -> new ReadableDocumentWorkspaceRepository()
                        .load(tempDir.resolve("Proyecto.docupodcast.json")));

        assertTrue(error.getMessage().contains("Vuelve a importar el PDF"));
    }

    @Test
    void saveAsCarriesPreparedPagesStableIdsAndManualOverrides() throws Exception {
        Path importedSource = tempDir.resolve("entrada.pdf");
        Files.writeString(importedSource, "%PDF-test");
        Path originalRoot = tempDir.resolve("proyecto-original");
        Path copiedRoot = tempDir.resolve("proyecto-copia");
        JsonPreparedPdfDocumentRepository pdfRepository = new JsonPreparedPdfDocumentRepository();
        ReadableDocumentWorkspaceRepository repository = new ReadableDocumentWorkspaceRepository(pdfRepository);
        Path stagingRoot = tempDir.resolve("staging-original");
        pdfRepository.initialize(stagingRoot, manifest("Manual", importedSource, 1));
        PreparedPdfSource imported = new PreparedPdfSource(
                new PreparedPdfWorkspaceRef(stagingRoot, importedSource, sha256(importedSource)), "Manual");
        repository.materialize(imported, ReadingProfile.academicDefaults(),
                originalRoot.resolve("Proyecto.docupodcast.json"));
        pdfRepository.savePage(originalRoot, preparedPage());
        PreparedPdfSource preparedSource = (PreparedPdfSource) repository
                .load(originalRoot.resolve("Proyecto.docupodcast.json"))
                .orElseThrow();
        PreparedPdfPage currentPage = pdfRepository.loadPage(originalRoot, 1).orElseThrow();
        PdfRegion currentRegion = currentPage.regions().getFirst();
        PdfRegion corrected = new PdfRegion(currentRegion.id(), currentRegion.pageNumber(),
                currentRegion.xMin(), currentRegion.yMin(), currentRegion.xMax(), currentRegion.yMax(),
                currentRegion.columnIndex(), currentRegion.readingOrder(), currentRegion.text(),
                currentRegion.automaticType(), currentRegion.automaticNarratability(),
                currentRegion.reasons(), currentRegion.evidence(),
                new PdfRegionOverride(null, PdfRegionType.HEADING, null, null),
                currentRegion.attributes(), currentRegion.revision() + 1);
        pdfRepository.savePage(originalRoot, new PreparedPdfPage(
                currentPage.schemaVersion(), currentPage.pageNumber(), currentPage.widthPoints(),
                currentPage.heightPoints(), currentPage.status(), currentPage.revision() + 1,
                List.of(corrected), currentPage.derivedTreatments(), currentPage.preparationMetrics(),
                currentPage.warnings(), currentPage.lastAttemptError()));
        JsonPdfPageMapRepository pageMapRepository = new JsonPdfPageMapRepository();
        var originalPageMap = new BuildPdfPageMapUseCase().build(
                pdfRepository.loadPage(originalRoot, 1).orElseThrow(), null);
        pageMapRepository.savePage(originalRoot, preparedSource.workspace().sourceSha256(), originalPageMap);

        repository.materialize(preparedSource, ReadingProfile.academicDefaults(),
                copiedRoot.resolve("Proyecto.docupodcast.json"));

        PreparedPdfPage copiedPage = pdfRepository.loadPage(copiedRoot, 1).orElseThrow();
        PdfRegion copiedRegion = copiedPage.regions().getFirst();
        assertEquals("REGION-STABLE", copiedRegion.id());
        assertEquals(PdfRegionType.HEADING, copiedRegion.effectiveType());
        assertEquals(PdfNarratability.NARRATABLE, copiedRegion.effectiveNarratability());
        assertEquals(originalPageMap, pageMapRepository.loadPage(copiedRoot, 1).orElseThrow());
        assertFalse(Files.exists(copiedRoot.resolve("document/document.json")));
    }

    private static PreparedPdfPage preparedPage() {
        PdfRegion region = new PdfRegion(
                "REGION-STABLE", 1,
                20, 30, 500, 75,
                0, 0,
                "Texto narrable conservado.",
                PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE,
                List.of("prose-shape"),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.93,
                        "ocr-test", "parser-test", "group-test", "classifier-test"),
                PdfRegionOverride.empty(),
                Map.of(),
                1);
        return new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1,
                612,
                792,
                PdfPagePreparationStatus.READY,
                1,
                List.of(region),
                List.of(),
                "");
    }

    private static com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest
    manifest(String title, Path source, int pages) throws Exception {
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        return new com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest(
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest
                        .CURRENT_SCHEMA_VERSION,
                title, source.getFileName().toString(), sha256(source), pages,
                "test", now, now);
    }

    private static String sha256(Path source) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
    }
}
