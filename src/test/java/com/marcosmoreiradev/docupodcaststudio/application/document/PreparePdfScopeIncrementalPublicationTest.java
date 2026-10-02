package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.MockAudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PreparePdfScopeIncrementalPublicationTest {
    @TempDir Path temp;

    @Test
    void publishesFirstAcceptedPageBeforeTheWholeScopeCompletes() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        String sha = "6".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Dos páginas", "source.pdf",
                sha, 2, "v3", Instant.now(), Instant.now()));
        CountDownLatch secondPageMayFinish = new CountDownLatch(1);
        CountDownLatch firstAccepted = new CountDownLatch(1);
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            if (request.pageNumber() == 2) {
                try {
                    assertTrue(secondPageMayFinish.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return new PreparePdfPageResult(2, null, false, true, List.of());
                }
            }
            return new PreparePdfPageResult(request.pageNumber(),
                    emptyPage(request.pageNumber()), true, false, List.of());
        })) {
            PreparePdfScopeUseCase useCase = new PreparePdfScopeUseCase(
                    new ResolvePdfPreparationScopeUseCase(repository), scheduler);
            PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                    temp, temp.resolve("source/source.pdf"), sha);
            var completion = useCase.execute(new PdfPreparationScopeRequest(
                            workspace, PdfPreparationScope.PAGE_RANGE, 1, 1, 2, null),
                    temp.resolve("cache"), PdfPreparationPriority.URGENT,
                    new PdfProjectSessionToken(), ignored -> { }, result -> {
                        if (result.pageNumber() == 1) firstAccepted.countDown();
                    });

            assertTrue(firstAccepted.await(5, TimeUnit.SECONDS));
            assertFalse(completion.isDone(),
                    "La página aceptada debe poder iniciar TTS antes del allOf");
            secondPageMayFinish.countDown();
            assertEquals(List.of(1, 2), completion.get(5, TimeUnit.SECONDS).preparedPages());
        }
    }

    @Test
    void acceptedPageCanProduceItsFirstWavWhileQwenStageContinuesNextPage()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "5".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Pipeline", "source.pdf",
                sha, 2, "v3", Instant.now(), Instant.now()));
        CountDownLatch releaseSecondPage = new CountDownLatch(1);
        CountDownLatch firstWav = new CountDownLatch(1);
        MockAudioGenerationGateway audio = new MockAudioGenerationGateway(
                new com.marcosmoreiradev.docupodcaststudio.infrastructure.audio
                        .InMemoryAudioJobQueue(), 0L);
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source/source.pdf"), sha);
        BuildPreparedPdfNarrationUseCase narration =
                new BuildPreparedPdfNarrationUseCase(repository);
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            if (request.pageNumber() == 2) {
                try {
                    assertTrue(releaseSecondPage.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return new PreparePdfPageResult(2, null, false, true, List.of());
                }
            }
            PreparedPdfPage page = narratedPage(request.pageNumber());
            try {
                repository.savePage(temp, page);
            } catch (java.io.IOException failure) {
                throw new IllegalStateException(failure);
            }
            return new PreparePdfPageResult(request.pageNumber(), page,
                    true, false, List.of());
        })) {
            PreparePdfScopeUseCase useCase = new PreparePdfScopeUseCase(
                    new ResolvePdfPreparationScopeUseCase(repository), scheduler);
            var completion = useCase.execute(new PdfPreparationScopeRequest(
                            workspace, PdfPreparationScope.PAGE_RANGE, 1, 1, 2, null),
                    temp.resolve("cache"), PdfPreparationPriority.URGENT,
                    new PdfProjectSessionToken(), ignored -> { }, result -> {
                        if (result.pageNumber() != 1) return;
                        var pageScript = narration.buildPage(workspace, 1,
                                "Pipeline", "es", false, null, null);
                        audio.submit(new AudioGenerationRequest(pageScript, temp,
                                        "Página 1 incremental"),
                                status -> {
                                    if (status.completedSegments() > 0) firstWav.countDown();
                                });
                    });

            assertTrue(firstWav.await(5, TimeUnit.SECONDS),
                    "El primer WAV debe publicarse mientras la página 2 sigue en preparación");
            assertFalse(completion.isDone());
            releaseSecondPage.countDown();
            assertEquals(List.of(1, 2), completion.get(5, TimeUnit.SECONDS).preparedPages());
        }
    }

    @Test
    void acceptedRejectedAcceptedCompletesAsPartialSuccessAndCountsTerminalOutcomes()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository = repository(3);
        CopyOnWriteArrayList<PdfPreparationScopeProgress> progress =
                new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<Integer> published = new CopyOnWriteArrayList<>();
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            if (request.pageNumber() == 2) {
                return PreparePdfPageResult.rejected(2, null, List.of(),
                        "Página 2 no pudo interpretarse con suficiente fiabilidad.",
                        new PdfSemanticCoverageException("coverage", null));
            }
            return new PreparePdfPageResult(request.pageNumber(),
                    emptyPage(request.pageNumber()), true, false, List.of());
        })) {
            PdfPreparationScopeResult result = scope(repository, scheduler, 3)
                    .execute(scopeRequest(3), temp.resolve("cache"),
                            PdfPreparationPriority.URGENT,
                            new PdfProjectSessionToken(), progress::add,
                            page -> published.add(page.pageNumber()))
                    .get(5, TimeUnit.SECONDS);

            assertAll(
                    () -> assertEquals(PdfPreparationScopeResult.AggregateStatus.PARTIAL_SUCCESS,
                            result.aggregateStatus()),
                    () -> assertEquals(List.of(1, 3), result.preparedPages()),
                    () -> assertEquals(List.of(2), result.rejectedPages()),
                    () -> assertTrue(result.technicalFailurePages().isEmpty()),
                    () -> assertEquals(List.of(1, 3), published),
                    () -> assertEquals(3, progress.getLast().completedPages()),
                    () -> assertEquals(2, progress.getLast().acceptedPages()),
                    () -> assertEquals(1, progress.getLast().rejectedPages()),
                    () -> assertEquals("Página 3 preparada.",
                            progress.getLast().message()));
        }
    }

    @Test
    void pageLocalRuntimeFailurePreservesCauseAndDoesNotPreventFollowingPage()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository = repository(3);
        IllegalArgumentException root = new IllegalArgumentException("root-page-2");
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            if (request.pageNumber() == 2) throw root;
            return new PreparePdfPageResult(request.pageNumber(),
                    emptyPage(request.pageNumber()), true, false, List.of());
        })) {
            PdfPreparationScopeResult result = scope(repository, scheduler, 3)
                    .execute(scopeRequest(3), temp.resolve("cache"),
                            PdfPreparationPriority.URGENT,
                            new PdfProjectSessionToken(), ignored -> { })
                    .get(5, TimeUnit.SECONDS);
            PreparePdfPageResult page2 = result.pageOutcomes().stream()
                    .filter(outcome -> outcome.pageNumber() == 2)
                    .findFirst().orElseThrow();
            assertAll(
                    () -> assertEquals(List.of(1, 3), result.preparedPages()),
                    () -> assertEquals(List.of(2), result.technicalFailurePages()),
                    () -> assertSame(root, page2.diagnosticCause()),
                    () -> assertEquals("UNEXPECTED_RUNTIME_FAILURE",
                            page2.failureCategory()),
                    () -> assertFalse(page2.userFacingReason()
                            .contains("Páginas fallidas")));
        }
    }

    @Test
    void rejectedAttemptCanExposePreviousCanonicalPageWithoutBecomingAccepted() {
        PreparedPdfPage previous = emptyPage(2);
        PreparePdfPageResult result = PreparePdfPageResult.rejected(
                2, previous, List.of(), "No aceptada", null);
        assertAll(
                () -> assertFalse(result.succeeded()),
                () -> assertTrue(result.rejected()),
                () -> assertTrue(result.canonicalPageAvailable()),
                () -> assertSame(previous, result.preparedPage()));
    }

    @Test
    void globalBatchFailureRemainsExceptionalInsteadOfBeingReclassifiedAsPageRejection()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository = repository(1);
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(
                (request, priority) -> new PreparePdfPageResult(request.pageNumber(),
                        emptyPage(request.pageNumber()), true, false, List.of()),
                work -> { throw new IllegalStateException("global-runtime"); })) {
            var completion = scope(repository, scheduler, 1).execute(
                    scopeRequest(1), temp.resolve("cache"),
                    PdfPreparationPriority.URGENT,
                    new PdfProjectSessionToken(), ignored -> { });
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> completion.get(5, TimeUnit.SECONDS));
            assertEquals("global-runtime", failure.getCause().getMessage());
        }
    }

    private JsonPreparedPdfDocumentRepository repository(int pages)
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Scope", "source.pdf",
                "a".repeat(64), pages, "v3", Instant.now(), Instant.now()));
        return repository;
    }

    private PreparePdfScopeUseCase scope(JsonPreparedPdfDocumentRepository repository,
                                         PdfPagePreparationScheduler scheduler,
                                         int ignoredPages) {
        return new PreparePdfScopeUseCase(
                new ResolvePdfPreparationScopeUseCase(repository), scheduler);
    }

    private PdfPreparationScopeRequest scopeRequest(int pages) {
        return new PdfPreparationScopeRequest(new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source/source.pdf"), "a".repeat(64)),
                PdfPreparationScope.PAGE_RANGE, 1, 1, pages, null);
    }

    private static PreparedPdfPage emptyPage(int page) {
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                page, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(), List.of(), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), "");
    }

    private static PreparedPdfPage narratedPage(int page) {
        PdfRegion region = new PdfRegion("P" + page + "-R1", page,
                40, 100, 560, 160, 0, 0, "Texto de la página " + page + ".",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 1.0,
                "test", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                page, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(region), List.of(), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), "");
    }
}
