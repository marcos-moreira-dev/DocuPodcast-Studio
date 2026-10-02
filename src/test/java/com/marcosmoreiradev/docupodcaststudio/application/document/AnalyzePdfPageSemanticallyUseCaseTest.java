package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttempt;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttemptState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.BlockPdfSemanticPageResponseParser;
import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisEngine;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisEngineRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfigurationSchema;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.LocalResourceScheduler;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.PriorityResourceScheduler;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeAdmissionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeResourceDemand;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeWorkloadKind;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AnalyzePdfPageSemanticallyUseCaseTest {
    @TempDir Path temp;

    @Test
    void omittedProminentTitleUsesTargetedClassificationAndRunningHeaderIsSilent()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine("""
                PAGE|V1|es|CONTENT
                BEGIN|TITLE|0|0|980|35|N|95
                SOURCE
                La demostracion: encerrar lo desconocido entre dos certezas
                END
                BEGIN|PARAGRAPH|70|90|920|135|N|96
                SOURCE
                La idea geometrica permite comparar tres cantidades positivas.
                END
                DONE
                """);
        engine.recoveryResponse = """
                PAGE|V1|es|CONTENT
                BEGIN|TITLE|10|80|990|920|N|99
                SOURCE
                La demostracion: encerrar lo desconocido entre dos certezas
                END
                DONE
                """;
        PdfNativePageExtractor evidence = (source, page) -> switch (page) {
            case 1, 3 -> geometryLayer(page,
                    geometryLine(page,
                            "Un limite notable, visto de cerca Calculo diferencial",
                            44, 4, 552, 13));
            case 2 -> geometryLayer(page,
                    geometryLine(page,
                            "Un limite notable, visto de cerca Calculo diferencial",
                            44, 4, 552, 13),
                    geometryLine(page,
                            "La demostracion: encerrar lo desconocido entre dos certezas",
                            44, 38, 470, 53),
                    geometryLine(page,
                            "La idea geometrica permite comparar tres cantidades positivas.",
                            44, 72, 545, 82));
            default -> new PdfTextLayer(page, PdfTextLayerOrigin.UNAVAILABLE,
                    List.of(), List.of());
        };
        AnalyzePdfPageSemanticallyUseCase useCase = useCase(engine, evidence);
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "d".repeat(64));

        PreparedPdfPage result = useCase.analyze(workspace, 2, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertEquals(2, engine.calls);
        assertEquals(List.of("primary", "targeted-recovery"),
                engine.requests.stream().map(request -> request.options()
                        .get("semanticPass")).toList());
        assertEquals("PROMINENT_TEXT_UNCOVERED", engine.requests.getLast()
                .options().get("recoveryReason"));
        assertEquals("300", engine.requests.getLast().options()
                .get("maxOutputTokens"));
        assertTrue(engine.requests.getLast().instruction()
                .contains("septimo campo debe ser la letra N"));
        assertTrue(engine.requests.getLast().instruction()
                .contains("BEGIN|HEADING|80|100|920|900|N|98"));
        var header = result.regions().stream()
                .filter(region -> region.automaticType() == PdfRegionType.HEADER)
                .findFirst().orElseThrow();
        var title = result.regions().stream()
                .filter(region -> region.text().contains("encerrar lo desconocido"))
                .findFirst().orElseThrow();
        assertEquals(PdfNarratability.NON_NARRATABLE,
                header.automaticNarratability());
        assertEquals("false", header.attributes().get("playbackTarget"));
        assertEquals(PdfRegionType.TITLE, title.automaticType());
        assertTrue(title.readingOrder() < result.regions().stream()
                .filter(region -> region.automaticType() == PdfRegionType.PARAGRAPH)
                .findFirst().orElseThrow().readingOrder());
        assertEquals(1, result.regions().stream()
                .filter(region -> region.text().contains("encerrar lo desconocido"))
                .count());
    }

    @Test
    void readsCompletePageIntoCanonicalRegionsAndPreservesReviewedMeaning()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(response(
                "La tabla compara ingresos y costos."));
        AnalyzePdfPageSemanticallyUseCase useCase = useCase(engine);
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "d".repeat(64));

        PreparedPdfPage first = useCase.analyze(workspace, 1, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertEquals(1, engine.calls);
        assertTrue(engine.visualExistedDuringAnalysis);
        assertEquals(ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                engine.lastRequest.operation());
        assertTrue(engine.lastRequest.responseSchema().isBlank());
        assertEquals("8192", engine.lastRequest.options()
                .get("contextWindowTokens"));
        assertEquals("1800", engine.lastRequest.options()
                .get("maxOutputTokens"));
        assertEquals("page-semantic-block-v1", engine.lastRequest.options()
                .get("responseProtocol"));
        assertTrue(engine.lastRequest.instruction()
                .contains("listado de programa, instrucciones o ensamblador es CODE"));
        assertTrue(engine.lastRequest.instruction()
                .contains("anotacion lateral"));
        assertEquals(AnalyzePdfPageSemanticallyUseCase.RENDER_DPI,
                Integer.parseInt(first.regions().getFirst().attributes()
                        .get("renderDpi")));
        assertEquals(3, first.regions().size());
        var table = first.regions().getFirst();
        assertEquals(PdfRegionType.TABLE, table.automaticType());
        assertEquals(PdfRegionOrigin.VLM_SEMANTIC, table.evidence().origin());
        assertEquals(60.0, table.xMin(), 0.001);
        assertEquals(160.0, table.yMin(), 0.001);
        assertEquals(540.0, table.xMax(), 0.001);
        assertEquals(400.0, table.yMax(), 0.001);
        assertEquals(PdfNarratability.UNCERTAIN,
                table.automaticNarratability());
        assertEquals(1, first.derivedTreatments().size());
        assertEquals("La tabla compara ingresos y costos.",
                first.derivedTreatments().getFirst().derivedText());
        assertTrue(first.warnings().stream()
                .anyMatch(value -> value.contains("coverage-validation-not-configured")));
        assertTrue(useCase.current(first));

        var approved = first.derivedTreatments().getFirst()
                .withState(PdfDerivedTreatmentState.APPROVED);
        PreparedPdfPage reviewed = new PreparedPdfPage(
                first.schemaVersion(), first.pageNumber(), first.widthPoints(),
                first.heightPoints(), first.status(), first.revision(),
                first.regions(), List.of(approved), first.preparationMetrics(),
                first.analysisProfile(), first.warnings(), first.lastAttemptError());
        engine.response = response("Una reinterpretacion automatica distinta.");

        PreparedPdfPage regenerated = useCase.analyze(workspace, 1, reviewed,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.NORMAL);

        assertEquals(table.id(), regenerated.regions().getFirst().id());
        assertEquals(approved.id(),
                regenerated.derivedTreatments().getFirst().id());
        assertEquals(approved.derivedText(),
                regenerated.derivedTreatments().getFirst().derivedText());
        assertEquals(PdfDerivedTreatmentState.APPROVED,
                regenerated.derivedTreatments().getFirst().state());
        assertFalse(Files.exists(engine.lastVisualPath),
                "El PNG temporal debe liberarse despues de la inferencia");
    }

    @Test
    void persistsInFlightCheckpointsBeforePublishingTheTerminalAttempt()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(response(
                "La tabla compara ingresos y costos."));
        RecordingAttemptRepository attempts = new RecordingAttemptRepository();
        AnalyzePdfPageSemanticallyUseCase useCase = useCase(engine, null, attempts);
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "d".repeat(64));

        useCase.analyze(workspace, 1, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertTrue(attempts.saved.size() >= 3);
        PdfOperationAttempt created = attempts.saved.getFirst();
        PdfOperationAttempt readiness = attempts.saved.stream()
                .filter(attempt -> attempt.state() == PdfOperationAttemptState.IN_FLIGHT)
                .filter(attempt -> attempt.diagnostic().contains("stage=READINESS"))
                .findFirst()
                .orElseThrow();
        PdfOperationAttempt terminal = attempts.saved.getLast();
        assertEquals(PdfOperationAttemptState.IN_FLIGHT, created.state());
        assertTrue(created.diagnostic().contains("stage=CREATED"));
        assertEquals(PdfOperationAttemptState.COMPLETED, terminal.state());
        assertEquals(created.id(), readiness.id());
        assertEquals(created.id(), terminal.id());
    }

    @Test
    void canonicalPagePreparationPersistsAndReusesSemanticResult()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(response(
                "La tabla compara ingresos y costos."));
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp, 1);
        BuildPdfOcrTextLayerUseCase ocr = new BuildPdfOcrTextLayerUseCase(
                request -> {
                    throw new AssertionError(
                            "El happy path semantico no debe iniciar OCR");
                });
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                ocr, null, null, repository, null, useCase(engine));

        PreparePdfPageResult first = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.URGENT));
        PreparePdfPageResult reopened = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.NORMAL));

        assertTrue(first.succeeded());
        assertTrue(reopened.succeeded());
        assertEquals(1, engine.calls);
        assertEquals(first.preparedPage().regions(),
                reopened.preparedPage().regions());
        assertTrue(reopened.issues().stream().anyMatch(issue ->
                issue.code().equals("pdf-semantic-page-reused")));
    }

    @Test
    void incompleteRegenerationKeepsThePreviousCanonicalPageAtomically()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(response(
                "La imagen compara ingresos y costos."));
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp, 1);
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                new BuildPdfOcrTextLayerUseCase(request -> {
                    throw new AssertionError("No debe iniciar OCR");
                }), null, null, repository, null, useCase(engine));

        PreparePdfPageResult initial = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.URGENT));
        assertTrue(initial.succeeded());
        PreparedPdfPage valid = initial.preparedPage();
        var oldProfile = valid.analysisProfile();
        var staleProfile = new com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile(
                oldProfile.automaticRole(), oldProfile.roleOverride(), oldProfile.language(),
                oldProfile.preparationProfile(), oldProfile.normalizedRotationDegrees(),
                List.of("legacy-semantic-reader"));
        PreparedPdfPage stale = new PreparedPdfPage(
                valid.schemaVersion(), valid.pageNumber(), valid.widthPoints(),
                valid.heightPoints(), valid.status(), valid.revision(), valid.regions(),
                valid.derivedTreatments(), valid.preparationMetrics(), staleProfile,
                valid.warnings(), "");
        repository.savePage(workspace.projectRoot(), stale);
        engine.response = "PAGE|V1|es|CONTENT\nBEGIN|PARAGRAPH|0|0|1000|200|N|90\nSOURCE\ntexto truncado";

        PreparePdfPageResult failed = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.NORMAL));

        assertFalse(failed.succeeded());
        PreparedPdfPage persisted = repository.loadPage(
                workspace.projectRoot(), 1).orElseThrow();
        assertEquals(stale.regions(), persisted.regions());
        assertEquals(stale.derivedTreatments(), persisted.derivedTreatments());
        assertEquals(stale.revision(), persisted.revision());
        assertTrue(persisted.lastAttemptError().contains("DONE"));
        assertTrue(failed.issues().stream().anyMatch(issue ->
                issue.code().equals("pdf-semantic-output-truncated")));
    }

    @Test
    void listeningAndProjectRestoreCannotRetryAPersistedTechnicalFailure()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(
                "PAGE|V1|es|CONTENT\nBEGIN|PARAGRAPH|0|0|1000|200|N|90\nSOURCE\nincompleto");
        RecordingAttemptRepository attempts = new RecordingAttemptRepository();
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp, 1);
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                new BuildPdfOcrTextLayerUseCase(request -> {
                    throw new AssertionError("No debe iniciar OCR");
                }), null, null, repository, null,
                useCase(engine, null, attempts));

        PreparePdfPageResult initial = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.URGENT,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                PdfPreparationOrigin.PROCESS_COMPLETE, true, "scope-process"));
        int callsAfterFailure = engine.calls;
        assertTrue(initial.technicalFailure());

        PreparePdfPageResult listen = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.URGENT,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                PdfPreparationOrigin.LISTEN_DOCUMENT, false, "scope-listen"));
        PreparePdfPageResult restore = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.NORMAL,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                PdfPreparationOrigin.PROJECT_RESTORE, false, "scope-restore"));

        assertEquals(callsAfterFailure, engine.calls);
        assertEquals("RETRY_NOT_AUTHORIZED", listen.failureCategory());
        assertEquals("RETRY_NOT_AUTHORIZED", restore.failureCategory());
    }

    @Test
    void explicitProcessingMayRetryAPersistedTechnicalFailure()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(
                "PAGE|V1|es|CONTENT\nBEGIN|PARAGRAPH|0|0|1000|200|N|90\nSOURCE\nincompleto");
        RecordingAttemptRepository attempts = new RecordingAttemptRepository();
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp, 1);
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                new BuildPdfOcrTextLayerUseCase(request -> {
                    throw new AssertionError("No debe iniciar OCR");
                }), null, null, repository, null,
                useCase(engine, null, attempts));
        PreparePdfPageRequest process = new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.URGENT,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                PdfPreparationOrigin.PROCESS_COMPLETE, true, "scope-process");

        prepare.execute(process);
        int firstAttemptCalls = engine.calls;
        prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.URGENT,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                PdfPreparationOrigin.EXPLICIT_RETRY, true, "scope-retry"));

        assertTrue(engine.calls > firstAttemptCalls);
    }

    @Test
    void coverageGapUsesOneDistinctVerifierAndPublishesOnlyAfterRevalidation()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine("""
                PAGE|V1|es|CONTENT
                BEGIN|PARAGRAPH|50|180|950|360|N|95
                SOURCE
                Este párrafo principal contiene información suficiente para la lectura semántica.
                END
                DONE
                """);
        engine.verificationResponse = """
                PAGE|V1|es|CONTENT
                BEGIN|TITLE|50|60|950|130|N|97
                SOURCE
                Título importante de la página
                END
                DONE
                """;
        PdfNativePageExtractor evidence = (source, page) -> nativeLayer(
                "Título importante de la página",
                "Este párrafo principal contiene información suficiente para la lectura semántica.");
        AnalyzePdfPageSemanticallyUseCase useCase = useCase(engine, evidence);
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "e".repeat(64));

        PreparedPdfPage page = useCase.analyze(workspace, 1, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertEquals(2, engine.calls);
        assertEquals("coverage-verification",
                engine.lastRequest.options().get("semanticPass"));
        assertEquals("1800",
                engine.lastRequest.options().get("maxOutputTokens"));
        assertTrue(engine.lastRequest.instruction()
                .contains("SALIDA SI HAY UNA CORRECCION"));
        assertTrue(engine.lastRequest.instruction()
                .contains("SOURCE nunca es opcional"));
        assertFalse(engine.lastRequest.instruction()
                .contains("devuelve regions vacio"));
        assertTrue(engine.lastRequest.instruction()
                .contains("CODE no se convierte en TABLE"));
        assertTrue(engine.lastRequest.instruction()
                .contains("Cada BEGIN tiene 8 campos"));
        assertTrue(engine.lastRequest.instruction()
                .contains("BEGIN|tipo|xMin|yMin|xMax|yMax|N/X/U|confianza"));
        assertTrue(engine.lastRequest.instruction()
                .contains("Esta prohibido abreviar un bloque"));
        assertFalse(engine.lastRequest.nearbyContext()
                .contains("PARAGRAPH ["));
        assertTrue(engine.lastRequest.instruction()
                .contains("linea posterior a CADA BEGIN"));
        assertTrue(engine.lastRequest.instruction()
                .contains("BEGIN|IMAGE|0|0|1000|1000|X|98"));
        assertEquals(2, page.regions().size());
        assertEquals(PdfRegionType.TITLE, page.regions().getFirst().automaticType());
        assertTrue(page.warnings().stream().anyMatch(value ->
                value.equals("semantic-coverage-status:ACCEPTED")));
    }

    @Test
    void giantTextualSupersegmentIsRefinedIntoPlaybackLeavesBeforePublication()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine("""
                PAGE|V1|es|CONTENT
                BEGIN|PARAGRAPH|20|20|980|950|N|97
                SOURCE
                Titulo de la pagina
                Primera idea independiente
                Segunda idea independiente
                Tercera idea independiente
                Cuarta idea independiente
                Conclusion independiente
                END
                DONE
                """);
        engine.recoveryResponse = """
                PAGE|V1|es|CONTENT
                BEGIN|TITLE|50|80|950|120|N|98
                SOURCE
                Titulo de la pagina
                END
                BEGIN|PARAGRAPH|50|180|950|230|N|97
                SOURCE
                Primera idea independiente
                END
                BEGIN|PARAGRAPH|50|300|950|350|N|97
                SOURCE
                Segunda idea independiente
                END
                BEGIN|PARAGRAPH|50|420|950|470|N|97
                SOURCE
                Tercera idea independiente
                END
                BEGIN|PARAGRAPH|50|540|950|590|N|97
                SOURCE
                Cuarta idea independiente
                END
                BEGIN|PARAGRAPH|50|660|950|710|N|97
                SOURCE
                Conclusion independiente
                END
                DONE
                """;
        PdfNativePageExtractor evidence = (source, page) -> nativeLayer(
                "Titulo de la pagina", "Primera idea independiente",
                "Segunda idea independiente", "Tercera idea independiente",
                "Cuarta idea independiente", "Conclusion independiente");

        PreparedPdfPage page = useCase(engine, evidence).analyze(
                new PreparedPdfWorkspaceRef(temp, temp.resolve("source.pdf"),
                        "f".repeat(64)),
                1, null, PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertEquals(2, engine.calls);
        assertEquals("SEGMENTATION_REFINEMENT",
                engine.lastRequest.options().get("recoveryReason"));
        assertEquals(1, page.regions().stream().filter(
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfRegion::container).count());
        assertEquals(6, page.regions().stream().filter(
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfRegion::playbackTarget).count());
        String parentId = page.regions().stream()
                .filter(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfRegion::container)
                .findFirst().orElseThrow().id();
        assertTrue(page.regions().stream()
                .filter(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfRegion::playbackTarget)
                .allMatch(region -> parentId.equals(region.parentId())));
        assertTrue(page.regions().stream()
                .filter(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfRegion::container)
                .allMatch(region -> region.effectiveText().isBlank()));
    }

    @Test
    void denseTableUsesOneTargetedCropAndPublishesAllVisibleCells()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine("""
                PAGE|V1|es|CONTENT
                BEGIN|TABLE|100|180|900|620|N|90
                SOURCE
                La tabla muestra resultados mensuales.
                END
                BEGIN|PARAGRAPH|100|700|900|820|N|96
                SOURCE
                Conclusión verificable del informe mensual.
                END
                DONE
                """);
        engine.verificationResponse = """
                PAGE|V1|es|CONTENT
                DONE
                """;
        engine.recoveryResponse = """
                PAGE|V1|es|CONTENT
                BEGIN|TABLE|0|0|1000|1000|N|98
                SOURCE
                Concepto ; Enero ; Febrero
                Ventas ; 1250 ; 1430
                Costos ; 710 ; 780
                END
                DONE
                """;
        PdfNativePageExtractor evidence = (source, page) -> nativeLayer(
                "Concepto Enero Febrero",
                "Ventas 1250 1430",
                "Costos 710 780",
                "Conclusión verificable del informe mensual");

        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(
                repository, temp, 1);
        PreparedPdfPage page = useCase(engine, evidence).analyze(
                workspace, 1, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertEquals(3, engine.calls);
        assertEquals("targeted-recovery",
                engine.lastRequest.options().get("semanticPass"));
        assertEquals("DENSE_TABLE_INCOMPLETE",
                engine.lastRequest.options().get("recoveryReason"));
        assertTrue(engine.lastRequest.instruction()
                .contains("BEGIN|TABLE|0|0|1000|1000|N|98"));
        assertFalse(engine.lastRequest.instruction().contains("Encabezado A"));
        assertTrue(engine.lastRequest.visualInputs().getFirst().role()
                .contains("targeted-recovery"));
        assertEquals(2, page.regions().size());
        assertTrue(page.regions().getFirst().text().contains("Ventas ; 1250 ; 1430"));
        assertTrue(page.regions().getFirst().text().contains("Costos ; 710 ; 780"));
        assertEquals("targeted-recovery",
                page.regions().getFirst().attributes().get("semanticPass"));
        assertFalse(Files.exists(engine.lastVisualPath));

        repository.savePage(workspace.projectRoot(), page);
        var script = new com.marcosmoreiradev.docupodcaststudio.application.script
                .BuildPreparedPdfNarrationUseCase(repository)
                .buildPage(workspace, 1, "Tabla recuperada", "es",
                        false, null, null);
        assertFalse(script.segments().isEmpty());
        assertTrue(script.segments().stream().anyMatch(segment ->
                segment.narrationText().contains("Conclusión verificable")));
    }

    @Test
    void visualRecoveryRequestsImageAndExactSpeechDelimiter() throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine("""
                PAGE|V1|es|CONTENT
                BEGIN|IMAGE|200|150|780|340|N|95
                SOURCE
                y = sin x / x; hueco en x = 0
                END
                DONE
                """);
        engine.verificationResponse = "PAGE|V1|es|CONTENT\nDONE";
        engine.recoveryResponse = """
                PAGE|V1|es|CONTENT
                BEGIN|IMAGE|0|0|1000|1000|N|98
                SOURCE
                y = sin x / x; hueco en x = 0
                SPEECH
                La curva se aproxima a uno desde ambos lados y deja un hueco en x igual a cero.
                END
                DONE
                """;
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(
                repository, temp, 1);
        PdfNativePageExtractor evidence = (source, pageNumber) -> nativeLayer(
                "y = sin x / x; hueco en x = 0");

        PreparedPdfPage page = useCase(engine, evidence).analyze(workspace, 1, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);

        assertEquals(3, engine.calls);
        assertEquals("VISUAL_REGION_INCOMPLETE",
                engine.lastRequest.options().get("recoveryReason"));
        assertTrue(engine.lastRequest.instruction()
                .contains("BEGIN|IMAGE|0|0|1000|1000|X|98"));
        assertTrue(engine.lastRequest.instruction()
                .contains("SPEECH no puede limitarse a repetir SOURCE"));
        assertTrue(engine.lastRequest.instruction()
                .contains("No emitas END antes de"));
        assertTrue(page.regions().stream().anyMatch(region ->
                region.automaticType() == PdfRegionType.IMAGE));
        assertTrue(page.derivedTreatments().stream().anyMatch(treatment ->
                treatment.derivedText().contains("aproxima a uno")));
    }

    @Test
    void failedTargetedRecoveryKeepsPreviousCanonicalPageAtomically()
            throws Exception {
        String complete = """
                PAGE|V1|es|CONTENT
                BEGIN|TABLE|100|180|900|620|N|98
                SOURCE
                Concepto ; Enero ; Febrero
                Ventas ; 1250 ; 1430
                Costos ; 710 ; 780
                END
                DONE
                """;
        CapturingSemanticEngine engine = new CapturingSemanticEngine(complete);
        PdfNativePageExtractor evidence = (source, page) -> nativeLayer(
                "Concepto Enero Febrero", "Ventas 1250 1430", "Costos 710 780");
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        RecordingAttemptRepository attempts = new RecordingAttemptRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(
                repository, temp, 1);
        AnalyzePdfPageSemanticallyUseCase semanticReader =
                useCase(engine, evidence, attempts);
        PreparedPdfPage valid = semanticReader.analyze(workspace, 1,
                null, PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);
        var oldProfile = valid.analysisProfile();
        var staleProfile = new com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile(
                oldProfile.automaticRole(), oldProfile.roleOverride(), oldProfile.language(),
                oldProfile.preparationProfile(), oldProfile.normalizedRotationDegrees(),
                List.of("legacy-semantic-reader"));
        PreparedPdfPage stale = new PreparedPdfPage(valid.schemaVersion(),
                valid.pageNumber(), valid.widthPoints(), valid.heightPoints(),
                valid.status(), valid.revision(), valid.regions(),
                valid.derivedTreatments(), valid.preparationMetrics(), staleProfile,
                valid.warnings(), "");
        assertFalse(semanticReader.current(stale));
        repository.savePage(workspace.projectRoot(), stale);

        engine.response = """
                PAGE|V1|es|CONTENT
                BEGIN|TABLE|100|180|900|620|N|90
                SOURCE
                Resumen de resultados.
                END
                DONE
                """;
        engine.verificationResponse = "PAGE|V1|es|CONTENT\nDONE";
        engine.recoveryResponse = """
                PAGE|V1|es|CONTENT
                BEGIN|TABLE|0|0|1000|1000|N|90
                SOURCE
                La tabla confirma los resultados.
                END
                DONE
                """;
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                new BuildPdfOcrTextLayerUseCase(request -> {
                    throw new AssertionError("No debe iniciar OCR sin política de fallback");
                }), null, null, repository, null, semanticReader);

        PreparePdfPageResult failed = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.NORMAL));

        assertFalse(failed.succeeded());
        PreparedPdfPage persisted = repository.loadPage(
                workspace.projectRoot(), 1).orElseThrow();
        assertEquals(stale.regions(), persisted.regions());
        assertEquals(stale.revision(), persisted.revision());
        assertTrue(persisted.lastAttemptError().contains("ronda focalizada"));
        assertEquals(PdfOperationAttemptState.INSUFFICIENT_EVIDENCE,
                attempts.saved.getLast().state());
        String technicalDiagnostic = attempts.saved.getLast().diagnostic();
        assertTrue(technicalDiagnostic.contains("[primary.raw]"));
        assertTrue(technicalDiagnostic.contains("[primary.parser]"));
        assertTrue(technicalDiagnostic.contains("[primary.coverage.reasons]"));
        assertTrue(technicalDiagnostic.contains("[verifier.raw]"));
        assertTrue(technicalDiagnostic.contains("[verifier.parser]"));
        assertTrue(technicalDiagnostic.contains("[recovery.plan]"));
        assertTrue(technicalDiagnostic.contains("[recovery.raw]"));
        assertTrue(technicalDiagnostic.contains("[final.coverage.reasons]"));
        assertTrue(technicalDiagnostic.contains("[promptVersion]"));
        assertFalse(technicalDiagnostic.toLowerCase(java.util.Locale.ROOT)
                .contains("base64"));
    }

    @Test
    void rejectedHttpRequestCannotReplaceAnExistingCanonicalPage()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(response(
                "La tabla compara ingresos y costos."));
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(
                repository, temp, 1);
        AnalyzePdfPageSemanticallyUseCase semanticReader = useCase(engine);
        PreparedPdfPage valid = semanticReader.analyze(workspace, 1, null,
                PdfPreparationCancellationToken.NONE,
                PdfPreparationPriority.URGENT);
        var profile = valid.analysisProfile();
        PreparedPdfPage stale = new PreparedPdfPage(
                valid.schemaVersion(), valid.pageNumber(), valid.widthPoints(),
                valid.heightPoints(), valid.status(), valid.revision(),
                valid.regions(), valid.derivedTreatments(),
                valid.preparationMetrics(),
                new com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile(
                        profile.automaticRole(), profile.roleOverride(),
                        profile.language(), profile.preparationProfile(),
                        profile.normalizedRotationDegrees(),
                        List.of("legacy-semantic-reader")),
                valid.warnings(), "");
        repository.savePage(workspace.projectRoot(), stale);
        engine.failure = new com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException(
                com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode.REQUEST_REJECTED,
                "Ollama rechazó la petición.",
                Map.of("statusCode", "400", "response",
                        "{\"error\":\"invalid request\"}"));
        PreparePdfPageUseCase prepare = new PreparePdfPageUseCase(
                new BuildPdfOcrTextLayerUseCase(request -> {
                    throw new AssertionError("No debe iniciar OCR");
                }), null, null, repository, null, semanticReader);

        PreparePdfPageResult failed = prepare.execute(new PreparePdfPageRequest(
                workspace, null, 1, false, null,
                PdfPreparationPriority.NORMAL));

        assertFalse(failed.succeeded());
        assertEquals("SEMANTIC_REQUEST_REJECTED", failed.failureCategory());
        PreparedPdfPage persisted = repository.loadPage(
                workspace.projectRoot(), 1).orElseThrow();
        assertEquals(stale.regions(), persisted.regions());
        assertEquals(stale.revision(), persisted.revision());
    }

    @Test
    void deadlineCoversRenderAdmissionAndPersistsResourceTimeout()
            throws Exception {
        CapturingSemanticEngine engine = new CapturingSemanticEngine(response(
                "No debe invocarse."));
        RecordingAttemptRepository attempts = new RecordingAttemptRepository();
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                Map.of(ResourceId.CPU_HEAVY, 1));
        ContentAnalysisEngineRegistry registry =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaCapabilityService media = new MediaCapabilityService(
                new MediaEnginePlatform(null, null, null, null, null, null,
                        registry, null), scheduler);
        AnalyzePdfPageSemanticallyUseCase useCase =
                new AnalyzePdfPageSemanticallyUseCase(new FakeRenderEngine(),
                        media, new BlockPdfSemanticPageResponseParser(), attempts,
                        null, Duration.ofMillis(150));
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "d".repeat(64));

        try (ResourceLease ignored = scheduler.acquire(
                new ComputeAdmissionRequest("holder", "holder",
                        ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.OTHER,
                        ComputeResourceDemand.of(ResourceId.CPU_HEAVY),
                        CancellationToken.NONE))) {
            java.io.IOException failure = assertThrows(java.io.IOException.class,
                    () -> useCase.analyze(workspace, 1, null,
                            PdfPreparationCancellationToken.NONE,
                            PdfPreparationPriority.URGENT));
            assertTrue(failure.getMessage().contains(
                    "RESOURCE_ADMISSION_TIMEOUT"));
        }

        assertEquals(0, engine.calls);
        PdfOperationAttempt terminal = attempts.saved.getLast();
        assertEquals(PdfOperationAttemptState.FAILED, terminal.state());
        assertTrue(terminal.diagnostic().contains("stage=RESOURCE_ADMISSION"));
        assertTrue(terminal.diagnostic().contains(
                "code=RESOURCE_ADMISSION_TIMEOUT"));
        assertTrue(scheduler.snapshot().queued().isEmpty());
        assertTrue(scheduler.snapshot().active().isEmpty());
    }

    @Test
    void cleanupRemovesTheWholeManagedRequestTree() throws Exception {
        Path request = Files.createDirectory(temp.resolve("request"));
        Path nested = Files.createDirectories(request.resolve("roi/nested"));
        Files.writeString(request.resolve("page.png"), "page");
        Files.writeString(nested.resolve("crop.png"), "crop");

        AnalyzePdfPageSemanticallyUseCase.cleanupTemporaryDirectory(request);

        assertFalse(Files.exists(request));
    }

    private AnalyzePdfPageSemanticallyUseCase useCase(
            CapturingSemanticEngine engine) {
        return useCase(engine, null);
    }

    private AnalyzePdfPageSemanticallyUseCase useCase(
            CapturingSemanticEngine engine,
            PdfNativePageExtractor evidence) {
        return useCase(engine, evidence, PdfOperationAttemptRepository.disabled());
    }

    private AnalyzePdfPageSemanticallyUseCase useCase(
            CapturingSemanticEngine engine,
            PdfNativePageExtractor evidence,
            PdfOperationAttemptRepository attempts) {
        ContentAnalysisEngineRegistry registry =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaCapabilityService media = new MediaCapabilityService(
                new MediaEnginePlatform(null, null, null, null, null, null,
                        registry, null),
                LocalResourceScheduler.safeDefaults());
        return new AnalyzePdfPageSemanticallyUseCase(
                new FakeRenderEngine(), media,
                new BlockPdfSemanticPageResponseParser(),
                attempts, evidence);
    }

    private static PdfTextLayer nativeLayer(String... text) {
        java.util.ArrayList<PdfTextLine> lines = new java.util.ArrayList<>();
        double y = 80;
        for (String value : text) {
            PdfPageRegion region = new PdfPageRegion(1, 50, y, 550,
                    y + 24, 600, 800);
            lines.add(new PdfTextLine(1, value, region,
                    List.of(new PdfTextToken(value, region, 1.0)), 1.0));
            y += 100;
        }
        return new PdfTextLayer(1, PdfTextLayerOrigin.NATIVE_BBOX,
                lines, List.of());
    }

    private static PdfTextLayer geometryLayer(int page, PdfTextLine... lines) {
        return new PdfTextLayer(page, PdfTextLayerOrigin.NATIVE_BBOX,
                List.of(lines), List.of());
    }

    private static PdfTextLine geometryLine(int page, String text,
                                            double x1, double y1,
                                            double x2, double y2) {
        PdfPageRegion region = new PdfPageRegion(page, x1, y1, x2, y2,
                600, 800);
        return new PdfTextLine(page, text, region,
                List.of(new PdfTextToken(text, region, 0.99)), 0.99);
    }

    private static String response(String narration) {
        return """
                PAGE|V1|es|CONTENT
                BEGIN|TABLE|100|200|900|500|U|95
                SOURCE
                Concepto ; Ingresos ; Costos
                Enero ; 1250 ; 710
                END
                BEGIN|IMAGE|100|520|900|700|N|94
                SOURCE
                Grafico de ingresos y costos.
                SPEECH
                %s
                END
                BEGIN|PARAGRAPH|0|720|950|900|N|93
                SOURCE
                Conclusiones del periodo.
                END
                DONE
                """.formatted(narration);
    }

    private static final class FakeRenderEngine implements PdfRenderEngine {
        @Override
        public PdfDocumentInfo inspect(Path sourcePdf, PdfOpenOptions options) {
            return PdfDocumentInfo.unavailable(sourcePdf, 1, "test");
        }

        @Override
        public PdfPageRenderResult renderPage(PdfPageRenderRequest request) {
            return new PdfPageRenderResult(request.pageNumber(), 1,
                    600, 800, request.dpi(),
                    new BufferedImage(1200, 1600, BufferedImage.TYPE_INT_RGB),
                    "test", List.of());
        }

        @Override
        public PdfPageRenderResult renderCrop(PdfCropRenderRequest request) {
            return new PdfPageRenderResult(request.pageNumber(), 1,
                    600, 800, request.dpi(),
                    new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB),
                    "test-crop", List.of());
        }
    }

    private static final class CapturingSemanticEngine
            implements ContentAnalysisEngine {
        private String response;
        private String verificationResponse;
        private String recoveryResponse;
        private int calls;
        private boolean visualExistedDuringAnalysis;
        private Path lastVisualPath;
        private ContentAnalysisRequest lastRequest;
        private final java.util.ArrayList<ContentAnalysisRequest> requests =
                new java.util.ArrayList<>();
        private com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException failure;

        private CapturingSemanticEngine(String response) {
            this.response = response;
        }

        @Override
        public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("semantic-test"),
                    CapabilityId.VISUAL_CONTENT_DESCRIPTION, "test", "1",
                    "test", Set.of(), true);
        }

        @Override
        public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.PAGE_SEMANTIC_READING);
        }

        @Override
        public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(descriptor().id(), List.of());
        }

        @Override
        public EngineReadiness inspectReadiness(
                EngineConfiguration configuration) {
            return EngineReadiness.ready(descriptor().id(), "ready");
        }

        @Override
        public ContentAnalysisResult analyze(
                ContentAnalysisRequest request, ExecutionContext context)
                throws java.io.IOException {
            calls++;
            if (failure != null) throw failure;
            lastRequest = request;
            requests.add(request);
            lastVisualPath = request.visualInputs().getFirst().file();
            visualExistedDuringAnalysis = Files.isRegularFile(lastVisualPath);
            String pass = request.options().get("semanticPass");
            String output = "coverage-verification".equals(pass)
                    && verificationResponse != null ? verificationResponse
                    : "targeted-recovery".equals(pass)
                    && recoveryResponse != null ? recoveryResponse : response;
            return new ContentAnalysisResult("", output, 0.0, List.of(),
                    Map.of("engineId", "semantic-test", "model", "qwen-test"));
        }
    }

    private static final class RecordingAttemptRepository
            implements PdfOperationAttemptRepository {
        private final java.util.ArrayList<PdfOperationAttempt> saved =
                new java.util.ArrayList<>();

        @Override
        public List<PdfOperationAttempt> list(Path projectRoot) {
            java.util.LinkedHashMap<String, PdfOperationAttempt> latestById =
                    new java.util.LinkedHashMap<>();
            saved.forEach(attempt -> latestById.put(attempt.id(), attempt));
            return List.copyOf(latestById.values());
        }

        @Override
        public void save(Path projectRoot, PdfOperationAttempt attempt) {
            saved.add(attempt);
        }
    }
}
