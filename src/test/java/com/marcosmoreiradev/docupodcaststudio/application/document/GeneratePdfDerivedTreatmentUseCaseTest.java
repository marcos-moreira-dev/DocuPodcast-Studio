package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfOperationAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class GeneratePdfDerivedTreatmentUseCaseTest {
    @TempDir
    Path root;

    @Test
    void parsesOneConservativeNarratabilityDecisionPerStableRegionId() throws Exception {
        PdfRegion prose = region("R-1", "Una explicación completa.",
                PdfNarratability.NARRATABLE);
        PdfRegion noise = region("R-2", "| ; l1I ???",
                PdfNarratability.UNCERTAIN);
        String json = """
                {"decisions":[
                  {"regionId":"R-1","narratability":"NARRATABLE",
                   "reason":"CLEAR_PROSE","confidence":0.97},
                  {"regionId":"R-2","narratability":"NON_NARRATABLE",
                   "reason":"OCR_GIBBERISH","confidence":0.91}
                ],"uncertainties":[],"confidence":0.94}
                """;

        var decisions = TransversalNarratabilityReviewPdfTreatmentEngine.decisions(
                json, List.of(prose, noise));

        assertEquals(PdfNarratability.NARRATABLE,
                decisions.get("R-1").narratability());
        assertEquals(PdfNarratability.NON_NARRATABLE,
                decisions.get("R-2").narratability());
        assertEquals("OCR_GIBBERISH", decisions.get("R-2").reason());
        assertEquals(0.91, decisions.get("R-2").confidence());
    }

    @Test
    void makesMissingDuplicateLowConfidenceAndContradictoryDecisionsUncertain()
            throws Exception {
        List<PdfRegion> regions = List.of(
                region("R-1", "Texto uno.", PdfNarratability.NARRATABLE),
                region("R-2", "Texto dos.", PdfNarratability.NARRATABLE),
                region("R-3", "Texto tres.", PdfNarratability.NARRATABLE),
                region("R-4", "Texto cuatro.", PdfNarratability.NARRATABLE));
        String json = """
                {"decisions":[
                  {"regionId":"R-1","narratability":"NARRATABLE",
                   "reason":"CLEAR_PROSE","confidence":0.50},
                  {"regionId":"R-2","narratability":"NARRATABLE",
                   "reason":"OCR_GIBBERISH","confidence":0.95},
                  {"regionId":"R-3","narratability":"NARRATABLE",
                   "reason":"CLEAR_PROSE","confidence":0.95},
                  {"regionId":"R-3","narratability":"NON_NARRATABLE",
                   "reason":"OCR_GIBBERISH","confidence":0.95}
                ]}
                """;

        var decisions = TransversalNarratabilityReviewPdfTreatmentEngine.decisions(
                json, regions);

        assertEquals(List.of("R-1", "R-2", "R-3", "R-4"),
                decisions.keySet().stream().toList());
        assertTrue(decisions.values().stream().allMatch(
                decision -> decision.narratability() == PdfNarratability.UNCERTAIN));
    }

    @Test
    void rejectsInventedRegionIds() {
        PdfRegion region = region("R-1", "Texto.", PdfNarratability.NARRATABLE);
        String json = """
                {"decisions":[{"regionId":"INVENTED","narratability":"NARRATABLE",
                 "reason":"CLEAR_PROSE","confidence":0.99}]}
                """;

        assertThrows(java.io.IOException.class,
                () -> TransversalNarratabilityReviewPdfTreatmentEngine.decisions(
                        json, List.of(region)));
    }

    @Test
    void splitsLargeCompactInputsWithoutDroppingOrReorderingRegions() {
        List<PdfRegion> regions = java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(index -> region("R-" + index,
                        "Texto suficientemente largo para forzar varios fragmentos " + index,
                        PdfNarratability.NARRATABLE))
                .toList();

        List<List<PdfRegion>> groups =
                TransversalNarratabilityReviewPdfTreatmentEngine.contextGroups(
                        regions, 512);

        assertTrue(groups.size() > 1);
        assertEquals(regions.stream().map(PdfRegion::id).toList(),
                groups.stream().flatMap(List::stream).map(PdfRegion::id).toList());
    }

    private static PdfRegion region(String id, String text,
                                    PdfNarratability narratability) {
        return new PdfRegion(id, 1, 10, 10, 100, 20, 0, 0,
                text, PdfRegionType.PARAGRAPH, narratability, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.5,
                        "ocr", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }

    @Test
    void generatesAndRegeneratesLocalDerivativeWithoutReplacingSourceRegion() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Tabla", "tabla.pdf",
                "b".repeat(64), 1, "test", now, now));
        PdfRegion region = new PdfRegion("T-1", 1, 10, 10, 500, 100, 0, 0,
                "Nombre | Valor\nA | 10\nB | 20", PdfRegionType.TABLE,
                PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 1.0,
                        "native", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
        repository.savePage(root, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(region), List.of(), ""));
        GeneratePdfDerivedTreatmentUseCase useCase = new GeneratePdfDerivedTreatmentUseCase(
                repository, new PdfDerivedTreatmentEngineRegistry(
                List.of(new RuleBasedSmallTableNarrationEngine())));
        PdfDerivedTreatmentGenerationRequest request = new PdfDerivedTreatmentGenerationRequest(
                root, 1, PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION, List.of("T-1"),
                RuleBasedSmallTableNarrationEngine.ID, true, Map.of());

        PdfDerivedTreatment first = useCase.execute(request);
        PdfDerivedTreatment second = useCase.execute(request);
        PreparedPdfPage stored = repository.loadPage(root, 1).orElseThrow();

        assertEquals(first.id(), second.id());
        assertEquals(1, stored.derivedTreatments().size());
        assertTrue(stored.derivedTreatments().getFirst().derivedText().contains("3 filas"));
        assertEquals("Nombre | Valor\nA | 10\nB | 20", stored.regions().getFirst().text());
    }

    @Test
    void coercesAnEngineApprovedResultToDraftBeforePublishing() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Figura", "figura.pdf", "e".repeat(64), 1,
                "test", now, now));
        PdfRegion source = new PdfRegion(
                "IMG-1", 1, 10, 10, 500, 300, 0, 0, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE,
                List.of(), new PdfRegionEvidence(
                PdfRegionOrigin.OCR_LOCAL, 0.8,
                "ocr", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
        repository.savePage(root, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(source), List.of(), ""));
        PdfDerivedTreatmentEngine unsafeEngine = new PdfDerivedTreatmentEngine() {
            @Override public String id() { return "unsafe-test-engine"; }
            @Override public String version() { return "1"; }
            @Override public PdfDerivedTreatmentKind kind() {
                return PdfDerivedTreatmentKind.IMAGE_DESCRIPTION;
            }
            @Override
            public PdfDerivedTreatment generate(
                    PreparedPdfPage page, List<PdfRegion> regions,
                    PdfDerivedTreatmentGenerationRequest request) {
                return new PdfDerivedTreatment(
                        "DER-UNSAFE", kind(), List.of("IMG-1"),
                        "Una figura generada por el motor.", id(), version(),
                        0.99, now, PdfDerivedTreatmentState.APPROVED,
                        1, "fingerprint", "prompt", Map.of());
            }
        };
        GeneratePdfDerivedTreatmentUseCase useCase =
                new GeneratePdfDerivedTreatmentUseCase(
                        repository, new PdfDerivedTreatmentEngineRegistry(
                        List.of(unsafeEngine)));

        PdfDerivedTreatment generated = useCase.execute(
                new PdfDerivedTreatmentGenerationRequest(
                        root, 1, PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                        List.of("IMG-1"), unsafeEngine.id(), true, Map.of()));
        PdfDerivedTreatment stored = repository.loadPage(root, 1).orElseThrow()
                .derivedTreatments().getFirst();

        assertEquals(PdfDerivedTreatmentState.DRAFT, generated.state());
        assertEquals(PdfDerivedTreatmentState.DRAFT, stored.state());
    }

    @Test
    void persistsMetricsAndResumesSameObjectWithoutDuplicateGeneration()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        JsonPdfOperationAttemptRepository journal =
                new JsonPdfOperationAttemptRepository();
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Figura", "figura.pdf", "f".repeat(64), 1,
                "test", now, now));
        PdfRegion source = new PdfRegion(
                "IMG-A8", 1, 10, 10, 500, 300, 0, 0, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL,
                0.8, "ocr", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
        repository.savePage(root, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1,
                List.of(source), List.of(), ""));
        AtomicInteger calls = new AtomicInteger();
        PdfDerivedTreatmentEngine engine = new PdfDerivedTreatmentEngine() {
            @Override public String id() { return "a8-counting"; }
            @Override public String version() { return "1"; }
            @Override public PdfDerivedTreatmentKind kind() {
                return PdfDerivedTreatmentKind.IMAGE_DESCRIPTION;
            }
            @Override public PdfDerivedTreatment generate(
                    PreparedPdfPage page, List<PdfRegion> regions,
                    PdfDerivedTreatmentGenerationRequest request) {
                calls.incrementAndGet();
                return new PdfDerivedTreatment(
                        "DER-A8", kind(), List.of("IMG-A8"),
                        "Descripción revisable.", "model-a8", version(),
                        0.9, now, PdfDerivedTreatmentState.DRAFT, 1,
                        "evidence-a8", "prompt", Map.of(
                        "durationMs", "42", "ttftMs", "7",
                        "promptTokens", "11", "outputTokens", "5",
                        "contextChars", "120", "doneReason", "stop"));
            }
        };
        var registry = new PdfDerivedTreatmentEngineRegistry(List.of(engine));
        PdfDerivedTreatmentGenerationRequest request =
                new PdfDerivedTreatmentGenerationRequest(
                        root, 1, PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                        List.of("IMG-A8"), engine.id(), true,
                        Map.of("roiWidth", "800", "roiHeight", "600"));

        PdfDerivedTreatment first = new GeneratePdfDerivedTreatmentUseCase(
                repository, registry, journal).execute(request);
        PdfDerivedTreatment resumed = new GeneratePdfDerivedTreatmentUseCase(
                repository, registry, journal).execute(request);
        List<PdfOperationAttempt> persisted = journal.list(root);

        assertEquals(first.id(), resumed.id());
        assertEquals(1, calls.get());
        assertEquals(1, persisted.size());
        assertEquals(PdfOperationAttemptState.COMPLETED,
                persisted.getFirst().state());
        assertEquals(42, persisted.getFirst().metrics().durationMillis());
        assertEquals(7, persisted.getFirst().metrics().ttftMillis());
        assertEquals(11, persisted.getFirst().metrics().promptTokens());
        assertEquals(800, persisted.getFirst().metrics().widthPixels());
        assertEquals("stop", persisted.getFirst().metrics().doneReason());
    }

    @Test
    void refusesImplicitExecution() {
        GeneratePdfDerivedTreatmentUseCase useCase = new GeneratePdfDerivedTreatmentUseCase(
                new JsonPreparedPdfDocumentRepository(),
                new PdfDerivedTreatmentEngineRegistry(List.of(new RuleBasedSmallTableNarrationEngine())));
        PdfDerivedTreatmentGenerationRequest request = new PdfDerivedTreatmentGenerationRequest(
                root, 1, PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION, List.of("T-1"),
                RuleBasedSmallTableNarrationEngine.ID, false, Map.of());

        assertThrows(IllegalStateException.class, () -> useCase.execute(request));
    }

    @Test
    void selectedNarratabilityReviewOnlyChangesItsRequestedRegionAfterApproval() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Página", "pagina.pdf", "c".repeat(64), 1, "test", now, now));
        PdfRegion prose = region("R-1", 10, "Esta es una oración comprensible.");
        PdfRegion noise = region(
                "R-2", 80, "xqz | 1lI || ?", PdfNarratability.UNCERTAIN);
        repository.savePage(root, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(prose, noise), List.of(), ""));
        PdfDerivedTreatmentEngine semanticFilter = new PdfDerivedTreatmentEngine() {
            @Override public String id() { return "test-semantic-filter"; }
            @Override public String version() { return "1"; }
            @Override public PdfDerivedTreatmentKind kind() {
                return PdfDerivedTreatmentKind.NARRATABILITY_REVIEW;
            }
            @Override
            public PdfDerivedTreatment generate(PreparedPdfPage page, List<PdfRegion> regions,
                                                PdfDerivedTreatmentGenerationRequest request) {
                assertEquals(List.of("R-2"),
                        regions.stream().map(PdfRegion::id).toList());
                return new PdfDerivedTreatment("FILTER-1", kind(),
                        regions.stream().map(PdfRegion::id).toList(),
                        "Una región se propone como galimatías.", "qwen-test", version(),
                        0.9, now, PdfDerivedTreatmentState.DRAFT, 1, "fingerprint",
                        "classify", Map.of(
                        "decision.R-2", PdfNarratability.NON_NARRATABLE.name()));
            }
        };
        GeneratePdfDerivedTreatmentUseCase generate = new GeneratePdfDerivedTreatmentUseCase(
                repository, new PdfDerivedTreatmentEngineRegistry(List.of(semanticFilter)));

        PdfDerivedTreatment draft = generate.execute(new PdfDerivedTreatmentGenerationRequest(
                root, 1, PdfDerivedTreatmentKind.NARRATABILITY_REVIEW, List.of("R-2"),
                semanticFilter.id(), true, Map.of()));

        PreparedPdfPage beforeApproval = repository.loadPage(root, 1).orElseThrow();
        assertEquals(PdfNarratability.UNCERTAIN,
                beforeApproval.regions().get(1).effectiveNarratability(),
                "a draft must not alter the canonical/effective page");

        new UpdatePdfDerivedTreatmentUseCase(repository).review(
                root, 1, draft.id(), PdfDerivedTreatmentState.APPROVED);

        PreparedPdfPage approved = repository.loadPage(root, 1).orElseThrow();
        assertEquals("xqz | 1lI || ?", approved.regions().get(1).text());
        assertEquals(PdfNarratability.NON_NARRATABLE,
                approved.regions().get(1).effectiveNarratability());
        assertEquals(PdfNarratability.NARRATABLE,
                approved.regions().getFirst().effectiveNarratability());
    }

    @Test
    void reviewOrUpsertRepairsAProposalThatWasLostBeforeApproval() throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        Instant now = Instant.parse("2026-07-30T00:00:00Z");
        repository.initialize(root, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                "Página", "pagina.pdf", "d".repeat(64), 1,
                "test", now, now));
        PdfRegion doubtful = region(
                "R-2", 80, "xqz | 1lI || ?", PdfNarratability.UNCERTAIN);
        repository.savePage(root, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(doubtful), List.of(), ""));
        PdfDerivedTreatment lostProposal = new PdfDerivedTreatment(
                "FILTER-LOST",
                PdfDerivedTreatmentKind.NARRATABILITY_REVIEW,
                List.of("R-2"),
                "Se omite ruido OCR.",
                "local-test",
                "1",
                0.95,
                now,
                PdfDerivedTreatmentState.DRAFT,
                1,
                "fingerprint",
                "classify",
                Map.of("decision.R-2",
                        PdfNarratability.NON_NARRATABLE.name()));

        new UpdatePdfDerivedTreatmentUseCase(repository).reviewOrUpsert(
                root, 1, lostProposal, PdfDerivedTreatmentState.APPROVED);

        PreparedPdfPage repaired = repository.loadPage(root, 1).orElseThrow();
        assertEquals(1, repaired.derivedTreatments().size());
        assertEquals(PdfDerivedTreatmentState.APPROVED,
                repaired.derivedTreatments().getFirst().state());
        assertEquals(PdfNarratability.NON_NARRATABLE,
                repaired.regions().getFirst().effectiveNarratability());
    }

    private static PdfRegion region(String id, double y, String text) {
        return region(id, y, text, PdfNarratability.NARRATABLE);
    }

    private static PdfRegion region(String id, double y, String text,
                                    PdfNarratability narratability) {
        return new PdfRegion(id, 1, 10, y, 500, y + 40, 0, (int) y,
                text, PdfRegionType.PARAGRAPH, narratability,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 1.0,
                "native", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
