package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfPageMapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResolvePdfPageMapUseCaseTest {
    @TempDir Path temp;

    @Test
    void disabledFeatureUsesV3AndDoesNotWriteSidecar() throws Exception {
        InMemoryPreparedPdfDocumentRepository v3 = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(v3, temp, 1);
        v3.savePage(temp, page(1));
        ResolvePdfPageMapUseCase useCase = new ResolvePdfPageMapUseCase(
                v3, new JsonPdfPageMapRepository(), new BuildPdfPageMapUseCase(), false);

        PdfPageMapResolution resolution = useCase.resolve(workspace, 1);

        assertEquals(PdfPageMapResolution.Source.V3_FALLBACK, resolution.source());
        assertFalse(Files.exists(temp.resolve("document/page-maps")));
    }

    @Test
    void enabledFeatureMigratesLazilyThenReadsRoundTrippedSidecar() throws Exception {
        InMemoryPreparedPdfDocumentRepository v3 = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(v3, temp, 1);
        v3.savePage(temp, page(1));
        ResolvePdfPageMapUseCase useCase = new ResolvePdfPageMapUseCase(
                v3, new JsonPdfPageMapRepository(), new BuildPdfPageMapUseCase(), true);

        PdfPageMapResolution first = useCase.resolve(workspace, 1);
        PdfPageMapResolution second = useCase.resolve(workspace, 1);

        assertEquals(PdfPageMapResolution.Source.LAZY_REBUILT, first.source());
        assertEquals(PdfPageMapResolution.Source.SIDECAR, second.source());
        assertEquals(first.pageMap(), second.pageMap());
        assertTrue(Files.isRegularFile(temp.resolve("document/page-maps/manifest.json")));
        assertTrue(Files.isRegularFile(temp.resolve("document/page-maps/page-000001.json")));
    }

    @Test
    void corruptSidecarFallsBackWithoutChangingV3() throws Exception {
        InMemoryPreparedPdfDocumentRepository v3 = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(v3, temp, 1);
        PreparedPdfPage page = page(1);
        v3.savePage(temp, page);
        Path mapDir = temp.resolve("document/page-maps");
        Files.createDirectories(mapDir);
        Files.writeString(mapDir.resolve("manifest.json"), "broken");
        ResolvePdfPageMapUseCase useCase = new ResolvePdfPageMapUseCase(
                v3, new JsonPdfPageMapRepository(), new BuildPdfPageMapUseCase(), true);

        PdfPageMapResolution resolution = useCase.resolve(workspace, 1);

        assertEquals(PdfPageMapResolution.Source.V3_FALLBACK, resolution.source());
        assertEquals(page, v3.loadPage(temp, 1).orElseThrow());
        assertFalse(resolution.warnings().isEmpty());
    }

    private static PreparedPdfPage page(long revision) {
        PdfRegion region = new PdfRegion("R1", 1, 72, 100, 420, 142, 0, 0,
                "Una oración estable.", PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 0.99,
                "e", "p", "g", "c"), PdfRegionOverride.empty(), Map.of(), 1);
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, revision, List.of(region), List.of(), "");
    }
}
