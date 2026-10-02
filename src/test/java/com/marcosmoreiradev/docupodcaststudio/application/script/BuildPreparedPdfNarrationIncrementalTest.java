package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildPreparedPdfNarrationIncrementalTest {
    @TempDir Path temp;

    @Test
    void mergesAcceptedPagesInReadingOrderWithoutChangingStableIds() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        String sha = "7".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Incremental", "source.pdf",
                sha, 2, "v3", Instant.now(), Instant.now()));
        repository.savePage(temp, page(1, "P1-R1", "Primera página.", 1));
        repository.savePage(temp, page(2, "P2-R1", "Segunda página.", 1));
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source/source.pdf"), sha);
        BuildPreparedPdfNarrationUseCase useCase =
                new BuildPreparedPdfNarrationUseCase(repository);

        var page2 = useCase.buildPage(workspace, 2, "Incremental", "es",
                false, null, null);
        var page1 = useCase.buildPage(workspace, 1, "Incremental", "es",
                false, null, null);
        String page1Id = page1.segments().getFirst().id();
        var merged = useCase.mergePage(page2, page1, 1);

        assertEquals(List.of("Primera página.", "Segunda página."),
                merged.segments().stream().map(segment -> segment.narrationText()).toList());
        assertEquals(page2.id(), merged.id(), "El guion incremental conserva su identidad");
        assertEquals(page1Id, merged.segments().getFirst().id());
        assertEquals(page1Id, useCase.buildPage(workspace, 1, "Incremental", "es",
                false, null, null).segments().getFirst().id());
    }

    @Test
    void replacesOnlyTheChangedPageAndPreservesTheOtherPageAudioIdentity() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        String sha = "8".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Incremental", "source.pdf",
                sha, 2, "v3", Instant.now(), Instant.now()));
        repository.savePage(temp, page(1, "P1-R1", "Texto anterior.", 1));
        repository.savePage(temp, page(2, "P2-R1", "Texto estable.", 1));
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source/source.pdf"), sha);
        BuildPreparedPdfNarrationUseCase useCase =
                new BuildPreparedPdfNarrationUseCase(repository);
        var initial = useCase.build(workspace, "Incremental", "es", false, null);
        String stablePage2Id = initial.segments().get(1).id();

        repository.savePage(temp, page(1, "P1-R1", "Texto corregido.", 2));
        var updatedPage = useCase.buildPage(workspace, 1, "Incremental", "es",
                false, null, null);
        var merged = useCase.mergePage(initial, updatedPage, 1);

        assertEquals("Texto corregido.", merged.segments().getFirst().narrationText());
        assertEquals(stablePage2Id, merged.segments().get(1).id());
        assertEquals("Texto estable.", merged.segments().get(1).narrationText());
    }

    private static PreparedPdfPage page(int page, String regionId, String text,
                                        long revision) {
        PdfRegion region = new PdfRegion(regionId, page, 40, 100, 560, 160,
                0, 0, text, PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 1.0,
                        "qwen", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), revision);
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                page, 612, 792, PdfPagePreparationStatus.READY, revision,
                List.of(region), List.of(), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), "");
    }
}
