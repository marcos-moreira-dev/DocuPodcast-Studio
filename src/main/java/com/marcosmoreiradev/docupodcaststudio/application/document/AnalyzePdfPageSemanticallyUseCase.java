package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentLanguageProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationMetrics;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttempt;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttemptState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationMetrics;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPreparationProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.media.api.AnalysisVisualInput;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeAdmissionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeResourceDemand;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeWorkloadKind;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;
import com.marcosmoreiradev.docupodcaststudio.media.api.OperationDeadline;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceAdmissionTimeoutException;
import com.marcosmoreiradev.docupodcaststudio.media.api.PdfVlmRuntimeProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Complete-page VLM reader that publishes canonical PreparedPdfPage/PdfRegion data. */
public final class AnalyzePdfPageSemanticallyUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AnalyzePdfPageSemanticallyUseCase.class);
    public static final int RENDER_DPI = 180;
    public static final long MAX_PIXEL_COUNT = 36_000_000L;
    /** Exact compatibility marker for reusable persisted semantic pages. */
    public static final String SEMANTIC_SIGNATURE =
            "pdf-semantic-reader-leaf-layout-v4-prominent-text-2026-08-13";
    public static final String PROMPT_VERSION =
            "page-human-reading-block-v1-frozen-2026-08-10";
    private static final String VERIFIER_STRATEGY_VERSION =
            "coverage-verifier-operational-envelope-v2";
    public static final String TRANSPORT_VERSION = "page-semantic-block-v1";

    private static final String INSTRUCTION = """
            Lee visualmente toda la pagina como una persona. Devuelve TODOS los bloques semanticos
            utiles en orden fisico humano. Conserva literalmente texto, cifras, simbolos, formulas,
            codigo y rotulos dentro de diagramas. No omitas titulos, teoremas, demostraciones ni
            parrafos por contener notacion matematica. Omite solo decoracion, encabezados, pies y
            numeros de pagina repetitivos.

            Una tabla completa es una sola region TABLE y source contiene todas sus celdas visibles,
            con filas separadas por saltos y celdas por " ; ". bbox contiene cuatro enteros 0..1000.
            TABLE se usa solo cuando existen relaciones tabulares reales entre filas y celdas.
            Un listado de programa, instrucciones o ensamblador es CODE aunque este alineado en
            columnas. Conserva tambien cada comentario, llave y anotacion lateral que explique
            grupos de instrucciones; no transcribas solo la columna central del listado.
            source puede contener libremente JSON, codigo, Unicode, formulas, llaves y corchetes.
            speech es null para texto ordinario. Para IMAGE, source conserva todos los rotulos y
            speech describe relaciones visuales. Para MATH usa speech solo si agrega comprension.
            MATH es una formula independiente, no prosa con notacion. No inventes ni resumas.
            pageRole es CONTENT para una pagina normal de explicacion. Usa INDEX solo para un
            indice o tabla de contenidos, BIBLIOGRAPHY solo para referencias bibliograficas,
            CATALOG solo para un catalogo de entradas independientes y VISUAL_REFERENCE solo
            cuando la pagina sea principalmente una referencia visual. Si no es comprobable,
            usa UNKNOWN.

            Responde exclusivamente con esta gramatica de bloques V1, sin Markdown ni JSON:
            PAGE|V1|idioma|rol
            BEGIN|tipo|xMin|yMin|xMax|yMax|narratabilidad|confianza
            SOURCE
            contenido visible literal, en una o mas lineas
            SPEECH
            lectura derivada opcional, solo para MATH o IMAGE
            END
            DONE
            Emite BEGIN/SOURCE/[SPEECH]/END por cada region. tipo y rol usan los valores
            autorizados arriba; narratabilidad usa N, X o U; bbox y confianza son numeros.
            Cuando corresponda narracion visual, SPEECH debe ser una linea de control exacta,
            sin dos puntos, espacios ni texto adicional en esa misma linea.
            Cada BEGIN debe ir seguido inmediatamente por una linea SOURCE, incluso para texto
            ordinario. Esa linea debe contener exactamente SOURCE, sin dos puntos ni contenido
            adicional. DONE debe ser la ultima linea. SOURCE puede contener libremente JSON,
            codigo, llaves, corchetes, formulas y texto ordinario.
            Nunca emitas literalmente las palabras tipo, xMin, yMin, xMax, yMax,
            narratabilidad o confianza. Sustituyelas por valores reales. Ejemplo de respuesta
            completa valida:
            PAGE|V1|es|CONTENT
            BEGIN|PARAGRAPH|80|120|920|260|N|95
            SOURCE
            Texto visible de ejemplo.
            END
            DONE
            OBLIGATORIO: la primera linea de toda respuesta debe ser PAGE|V1|idioma|rol.
            No empieces directamente con BEGIN. Java descartara toda la respuesta si falta PAGE.
            """;

    private final PdfRenderEngine renderEngine;
    private final MediaCapabilityService media;
    private final PdfSemanticPageResponseParser responseParser;
    private final PdfOperationAttemptRepository attempts;
    private final PdfNativePageExtractor coverageEvidenceExtractor;
    private final Duration absoluteTimeout;
    private final PdfVlmRuntimeProfile vlmProfile;
    private final PdfNativeTextQualityAssessor nativeTextQuality =
            new PdfNativeTextQualityAssessor();
    private final PdfSemanticCoverageValidator coverageValidator =
            new PdfSemanticCoverageValidator();
    private final PdfSemanticRecoveryPlanner recoveryPlanner =
            new PdfSemanticRecoveryPlanner();
    private final PdfSemanticRecoveryMerger recoveryMerger =
            new PdfSemanticRecoveryMerger();
    private final PdfSemanticGranularityValidator granularityValidator =
            new PdfSemanticGranularityValidator();
    private final PdfRegionContentResolver contentResolver;
    private final PdfOcrEngine textGeometryOcrEngine;
    private final PdfSemanticNarrationSafetyValidator narrationSafety =
            new PdfSemanticNarrationSafetyValidator();
    private final PdfRepeatedMarginPolicy repeatedMarginPolicy =
            new PdfRepeatedMarginPolicy();
    private final PdfUncoveredProminentTextDetector prominentTextDetector =
            new PdfUncoveredProminentTextDetector();

    public AnalyzePdfPageSemanticallyUseCase(
            PdfRenderEngine renderEngine,
            MediaCapabilityService media,
            PdfSemanticPageResponseParser responseParser) {
        this(renderEngine, media, responseParser,
                PdfOperationAttemptRepository.disabled(), null, Duration.ofMinutes(20),
                PdfVlmRuntimeProfile.fromSystem());
    }

    public AnalyzePdfPageSemanticallyUseCase(
            PdfRenderEngine renderEngine,
            MediaCapabilityService media,
            PdfSemanticPageResponseParser responseParser,
            PdfOperationAttemptRepository attempts) {
        this(renderEngine, media, responseParser, attempts, null, Duration.ofMinutes(20),
                PdfVlmRuntimeProfile.fromSystem());
    }

    public AnalyzePdfPageSemanticallyUseCase(
            PdfRenderEngine renderEngine,
            MediaCapabilityService media,
            PdfSemanticPageResponseParser responseParser,
            PdfOperationAttemptRepository attempts,
            PdfNativePageExtractor coverageEvidenceExtractor) {
        this(renderEngine, media, responseParser, attempts,
                coverageEvidenceExtractor, Duration.ofMinutes(20),
                PdfVlmRuntimeProfile.fromSystem());
    }

    public AnalyzePdfPageSemanticallyUseCase(
            PdfRenderEngine renderEngine,
            MediaCapabilityService media,
            PdfSemanticPageResponseParser responseParser,
            PdfOperationAttemptRepository attempts,
            PdfNativePageExtractor coverageEvidenceExtractor,
            Duration absoluteTimeout) {
        this(renderEngine, media, responseParser, attempts, coverageEvidenceExtractor,
                absoluteTimeout, PdfVlmRuntimeProfile.fromSystem());
    }

    public AnalyzePdfPageSemanticallyUseCase(
            PdfRenderEngine renderEngine,
            MediaCapabilityService media,
            PdfSemanticPageResponseParser responseParser,
            PdfOperationAttemptRepository attempts,
            PdfNativePageExtractor coverageEvidenceExtractor,
            Duration absoluteTimeout,
            PdfVlmRuntimeProfile vlmProfile) {
        this(renderEngine, media, responseParser, attempts,
                coverageEvidenceExtractor, absoluteTimeout, vlmProfile, null);
    }

    public AnalyzePdfPageSemanticallyUseCase(
            PdfRenderEngine renderEngine,
            MediaCapabilityService media,
            PdfSemanticPageResponseParser responseParser,
            PdfOperationAttemptRepository attempts,
            PdfNativePageExtractor coverageEvidenceExtractor,
            Duration absoluteTimeout,
            PdfVlmRuntimeProfile vlmProfile,
            PdfOcrEngine regionOcrEngine) {
        this.renderEngine = Objects.requireNonNull(renderEngine, "renderEngine");
        this.media = Objects.requireNonNull(media, "media");
        this.responseParser = Objects.requireNonNull(responseParser, "responseParser");
        this.attempts = Objects.requireNonNullElseGet(
                attempts, PdfOperationAttemptRepository::disabled);
        this.coverageEvidenceExtractor = coverageEvidenceExtractor;
        this.absoluteTimeout = Objects.requireNonNull(absoluteTimeout, "absoluteTimeout");
        this.vlmProfile = Objects.requireNonNull(vlmProfile, "vlmProfile");
        this.textGeometryOcrEngine = regionOcrEngine;
        this.contentResolver = new PdfRegionContentResolver(regionOcrEngine);
    }

    public boolean current(PreparedPdfPage page) {
        return page != null
                && page.analysisProfile().analysisEngineIds().contains(SEMANTIC_SIGNATURE)
                && (page.regions().isEmpty() || page.regions().stream()
                .allMatch(region -> region.evidence().origin()
                        == PdfRegionOrigin.VLM_SEMANTIC));
    }

    /** Diagnostic only: classifies why an explicit scope request will run this page again. */
    String manualRetryReason(Path projectRoot, int pageNumber) {
        return lastAttemptState(projectRoot, pageNumber)
                .map(AnalyzePdfPageSemanticallyUseCase::manualRetryReason)
                .orElse("CANONICAL_CACHE_MISS");
    }

    java.util.Optional<PdfOperationAttemptState> lastAttemptState(
            Path projectRoot, int pageNumber) {
        try {
            return attempts.list(projectRoot).stream()
                    .filter(attempt -> attempt.pageNumber() == pageNumber)
                    .filter(attempt -> "PAGE_SEMANTIC_READING".equals(attempt.operation()))
                    .max(java.util.Comparator.comparing(
                            com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttempt::finishedAt))
                    .map(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                            .PdfOperationAttempt::state);
        } catch (IOException failure) {
            LOGGER.warn("pdf.semantic.retry-reason-unavailable page={} reason={}",
                    pageNumber, failure.toString());
            return java.util.Optional.empty();
        }
    }

    static String manualRetryReason(PdfOperationAttemptState state) {
        if (state == null) return "CANONICAL_CACHE_MISS";
        return switch (state) {
            case FAILED, TRUNCATED, INTERRUPTED -> "MANUAL_RETRY_AFTER_TECHNICAL_FAILURE";
            case INSUFFICIENT_EVIDENCE -> "MANUAL_RETRY_AFTER_SEMANTIC_REJECTION";
            case CANCELLED -> "MANUAL_RETRY_AFTER_CANCELLATION";
            default -> "CANONICAL_CACHE_MISS";
        };
    }

    public PreparedPdfPage analyze(PreparedPdfWorkspaceRef workspace,
                                   int pageNumber,
                                   PreparedPdfPage previous,
                                   PdfPreparationCancellationToken cancellation,
                                   PdfPreparationPriority priority)
            throws IOException, InterruptedException {
        return analyze(workspace, pageNumber, previous, cancellation, priority,
                ProgressSink.NONE, PdfPreparationOrigin.PROJECT_RESTORE, "", false);
    }

    public PreparedPdfPage analyze(PreparedPdfWorkspaceRef workspace,
                                   int pageNumber,
                                   PreparedPdfPage previous,
                                   PdfPreparationCancellationToken cancellation,
                                   PdfPreparationPriority priority,
                                   ProgressSink progress)
            throws IOException, InterruptedException {
        return analyze(workspace, pageNumber, previous, cancellation, priority,
                progress, PdfPreparationOrigin.PROJECT_RESTORE, "", false);
    }

    public PreparedPdfPage analyze(PreparedPdfWorkspaceRef workspace,
                                   int pageNumber,
                                   PreparedPdfPage previous,
                                   PdfPreparationCancellationToken cancellation,
                                   PdfPreparationPriority priority,
                                   ProgressSink progress,
                                   PdfPreparationOrigin origin,
                                   String scopeId,
                                   boolean retryAllowed)
            throws IOException, InterruptedException {
        Objects.requireNonNull(workspace, "workspace");
        PdfPreparationCancellationToken token = cancellation == null
                ? PdfPreparationCancellationToken.NONE : cancellation;
        ProgressSink stages = progress == null ? ProgressSink.NONE : progress;
        Instant attemptStarted = Instant.now();
        String attemptId = "PDF-SEM-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").toUpperCase(Locale.ROOT);
        long startedNanos = System.nanoTime();
        long startedCpu = currentThreadCpuNanos();
        long startedHeap = usedHeapBytes();
        OperationDeadline operationDeadline = OperationDeadline.after(absoluteTimeout);
        token.throwIfCancelled();
        recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                PdfOperationAttemptState.IN_FLIGHT, null, null,
                "stage=CREATED\norigin=" + Objects.toString(origin, "")
                        + "\nscopeId=" + Objects.toString(scopeId, "")
                        + "\nretryAllowed=" + retryAllowed
                        + "\nsourceSha=" + workspace.sourceSha256());
        Path directory = Files.createTempDirectory("docupodcast-semantic-page-");
        Path pageImage = directory.resolve("page.png");
        PdfPageRenderResult rendered = null;
        ContentAnalysisResult result = null;
        LOGGER.info("pdf.semantic.start page={} ctx=8192 output=1800 protocol={}",
                pageNumber, TRANSPORT_VERSION);
        try {
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.IN_FLIGHT, null, null,
                    "stage=RESOURCE_ADMISSION\norigin=" + Objects.toString(origin, "")
                            + "\nscopeId=" + Objects.toString(scopeId, "")
                            + "\nretryAllowed=" + retryAllowed
                            + "\nsourceSha=" + workspace.sourceSha256());
            stages.report("RENDERING", 0.05,
                    "Renderizando pagina " + pageNumber + ".");
            rendered = render(workspace, pageNumber, pageImage, token, priority,
                    stages, operationDeadline, () -> recordAttempt(
                            workspace, pageNumber, attemptId, attemptStarted,
                            PdfOperationAttemptState.IN_FLIGHT, null, null,
                            "stage=RASTERIZATION\norigin=" + Objects.toString(origin, "")
                                    + "\nscopeId=" + Objects.toString(scopeId, "")
                                    + "\nretryAllowed=" + retryAllowed
                                    + "\nsourceSha=" + workspace.sourceSha256()));
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.IN_FLIGHT, null, rendered,
                    "stage=RENDERED\norigin=" + Objects.toString(origin, "")
                            + "\nscopeId=" + Objects.toString(scopeId, "")
                            + "\nretryAllowed=" + retryAllowed
                            + "\nsourceSha=" + workspace.sourceSha256());
            stages.report("RENDERED", 0.18,
                    "Pagina " + pageNumber + " renderizada para lectura visual.");
            token.throwIfCancelled();
            ExecutionContext context = new ExecutionContext(
                    "semantic-pdf-page-" + pageNumber,
                    token::cancelled, stages, null, null)
                    .withDeadline(operationDeadline);
            PdfTextLayer nativeEvidence = coverageEvidence(workspace, pageNumber);
            PdfNativeTextQualityReport nativeReport =
                    nativeTextQuality.assess(nativeEvidence);
            PdfPageRenderResult completedRender = rendered;
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.IN_FLIGHT, null, rendered,
                    "stage=READINESS\norigin=" + Objects.toString(origin, "")
                            + "\nscopeId=" + Objects.toString(scopeId, "")
                            + "\nretryAllowed=" + retryAllowed
                            + "\nsourceSha=" + workspace.sourceSha256());
            SemanticInference inference = media.withContentAnalysisBatch(
                    context, ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                    vlmProfile.requestOptions(),
                    () -> inferSemantics(workspace, completedRender, directory,
                            pageImage, pageNumber, previous, token, priority,
                            stages, nativeEvidence, nativeReport, retryAllowed));
            result = inference.result();
            LOGGER.info("pdf.semantic.inference page={} durationMs={} ttftMs={} outputTokens={} tokPerSec={}",
                    pageNumber,
                    result.diagnostics().getOrDefault("durationMs", ""),
                    result.diagnostics().getOrDefault("timeToFirstTokenMs", ""),
                    result.diagnostics().getOrDefault("outputTokens", ""),
                    result.diagnostics().getOrDefault("generationTokensPerSecond", ""));
            PreparedPdfPage page = assemble(workspace, completedRender,
                    inference.analysis(), result, previous,
                    startedNanos, startedCpu, startedHeap);
            LOGGER.info("pdf.semantic.accepted page={} native={} coverage={} regions={} verifier={} recoveryRois={} durationMs={}",
                    pageNumber, nativeReport.quality(), inference.coverage().status(),
                    page.regions().size(), result.diagnostics().getOrDefault(
                            "coverageVerifierUsed", "false"),
                    result.diagnostics().getOrDefault(
                            "semanticRecoveryRoiCount", "0"),
                    result.diagnostics().getOrDefault("durationMs", ""));
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.COMPLETED, result, rendered,
                    successfulDiagnostic(result));
            stages.report("PARSED", 0.96,
                    "Pagina " + pageNumber + " interpretada en "
                            + page.regions().size() + " regiones.");
            return page;
        } catch (PdfSemanticProtocolException failure) {
            LOGGER.warn("pdf.semantic.protocol-failure page={} kind={} reason={}",
                    pageNumber, failure.kind(), failure.getMessage());
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    failure.kind() == PdfSemanticProtocolException.Kind.INCOMPLETE
                            ? PdfOperationAttemptState.TRUNCATED
                            : PdfOperationAttemptState.FAILED,
                    result, rendered, diagnostic(failure, result),
                    failure.diagnostics());
            throw failure;
        } catch (PdfPreparationCancelledException failure) {
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.CANCELLED, result, rendered,
                    diagnostic(failure, result));
            throw failure;
        } catch (ResourceAdmissionTimeoutException failure) {
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.FAILED, result, rendered,
                    "stage=RESOURCE_ADMISSION\ncode=RESOURCE_ADMISSION_TIMEOUT\n"
                            + "admissionId=" + failure.admissionId() + "\noperationId="
                            + failure.operationId() + "\nfitFailureReason="
                            + failure.fitFailureReason());
            throw new IOException("RESOURCE_ADMISSION_TIMEOUT: la pagina "
                    + pageNumber + " agotó su plazo esperando recursos.", failure);
        } catch (InterruptedException failure) {
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.CANCELLED, result, rendered,
                    diagnostic(failure, result));
            throw failure;
        } catch (PdfSemanticCoverageException failure) {
            LOGGER.info("pdf.semantic.rejected page={} reasons={}",
                    pageNumber, failure.result().reasons());
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted,
                    PdfOperationAttemptState.INSUFFICIENT_EVIDENCE,
                    result, rendered, diagnostic(failure, result),
                    failure.diagnostics());
            throw failure;
        } catch (IOException failure) {
            LOGGER.warn("pdf.semantic.engine-failure page={} type={} reason={}",
                    pageNumber, failure.getClass().getSimpleName(), failure.getMessage());
            PdfOperationAttemptState state = failure instanceof EngineExecutionException engine
                    && engine.code() == EngineDiagnosticCode.OUTPUT_TRUNCATED
                    ? PdfOperationAttemptState.TRUNCATED
                    : PdfOperationAttemptState.FAILED;
            recordAttempt(workspace, pageNumber, attemptId, attemptStarted, state,
                    result, rendered, diagnostic(failure, result),
                    failure instanceof EngineExecutionException engine
                            ? engine.diagnostics() : Map.of());
            throw failure;
        } finally {
            if (rendered != null) rendered.image().flush();
            cleanupTemporaryDirectory(directory);
            stages.report("CLEANUP", 1.0,
                    "Temporales de la pagina " + pageNumber + " liberados.");
            LOGGER.debug("pdf.semantic.cleanup page={} temp={}",
                    pageNumber, directory.getFileName());
        }
    }

    private SemanticInference inferSemantics(
            PreparedPdfWorkspaceRef workspace,
            PdfPageRenderResult rendered,
            Path temporaryDirectory,
            Path pageImage,
            int pageNumber,
            PreparedPdfPage previous,
            PdfPreparationCancellationToken token,
            PdfPreparationPriority priority,
            ProgressSink stages,
            PdfTextLayer nativeEvidence,
            PdfNativeTextQualityReport nativeReport,
            boolean retryAllowed)
            throws IOException, InterruptedException {
        LinkedHashMap<String, String> trace = new LinkedHashMap<>();
        trace.put("transportVersion", TRANSPORT_VERSION);
        trace.put("promptVersion", PROMPT_VERSION);
        trace.put("verifierStrategyVersion", VERIFIER_STRATEGY_VERSION);
        trace.put("runtimeProfile", "ctx=8192;primaryOutput=1800;verifierOutput=1800;batch=512");
        ContentAnalysisRequest primaryRequest = withRetryAuthority(semanticRequest(
                pageImage, pageNumber, previous, priority, INSTRUCTION,
                "Pagina solicitada: " + pageNumber + ".",
                "", "primary", "1800"), retryAllowed);
        ExecutionContext context = new ExecutionContext(
                "semantic-pdf-page-" + pageNumber,
                token::cancelled, stages, null, null);
        stages.report("INFERENCE", 0.22,
                "Analizando pagina " + pageNumber + ".");
        ContentAnalysisResult primary = media.analyzeContent(
                null, primaryRequest, context);
        captureStage(trace, "primary", primary);
        token.throwIfCancelled();
        stages.report("PARSING", 0.78,
                "Validando regiones de la pagina " + pageNumber + ".");
        PdfSemanticPageAnalysis analysis = withProvenance(parseWithDiagnostics(
                primary, "primary"), "primary", "");
        trace.put("primary.parser", analysisSummary(analysis));
        if (analysis.elements().isEmpty()) {
            throw new PdfSemanticProtocolException(
                    PdfSemanticProtocolException.Kind.INVALID,
                    "El envelope primario no contiene regiones.");
        }
        List<PdfSemanticPageAnalysis.Element> giantParents =
                granularityValidator.refinementCandidates(analysis, nativeEvidence);
        if (!giantParents.isEmpty()) {
            PdfSemanticPageAnalysis.Element parent = giantParents.getFirst();
            stages.report("SEGMENTATION_REFINEMENT", 0.80,
                    "Dividiendo una zona compuesta de la pagina " + pageNumber + ".");
            PdfSemanticRecoveryRoi roi = new PdfSemanticRecoveryRoi(
                    PdfSemanticRecoveryReason.SEGMENTATION_REFINEMENT,
                    parent.box(), parent.type(), List.of());
            RecoveryRound refinement = runTargetedRecovery(workspace, rendered,
                    temporaryDirectory, pageNumber, previous, token, priority,
                    stages, new PdfSemanticRecoveryPlan(List.of(roi)));
            if (refinement.analysis().elements().size() < 2) {
                throw new PdfSemanticCoverageException(
                        "SEGMENTATION_REFINEMENT no produjo hojas independientes.",
                        new PdfSemanticCoverageResult(
                                PdfSemanticCoverageStatus.REJECTED, false, 0.0,
                                List.of("giantPlaybackRegion"), List.of()),
                        refinement.rawOutput(), Map.of(
                        "failureStage", "SEGMENTATION_REFINEMENT"));
            }
            analysis = recoveryMerger.replacePlaybackParentWithChildren(
                    analysis, parent, refinement.analysis());
            trace.put("segmentationRefinementUsed", "true");
            trace.put("segmentationRefinementChildren", Integer.toString(
                    refinement.analysis().elements().size()));
        }
        PdfTextGeometryMap textGeometry = rasterTextGeometry(workspace, pageNumber,
                trace);
        PdfTextLayer coverageEvidence = coverageEvidence(nativeEvidence, textGeometry);
        PdfNativeTextQualityReport coverageQuality = textGeometry.available()
                ? new PdfNativeTextQualityReport(PdfNativeTextQuality.RELIABLE,
                1.0, List.of("OCR_RASTER_PAGE_GEOMETRY"))
                : nativeReport;
        PdfRegionContentResolver.Resolution content = contentResolver.resolve(
                analysis, nativeEvidence, textGeometry, workspace, rendered.pageWidthPoints(),
                rendered.pageHeightPoints(), workspace.projectRoot()
                        .resolve("document").resolve("ocr-cache"), pageNumber);
        analysis = content.analysis();
        trace.put("literalCropRegions", Integer.toString(content.literalRegions()));
        trace.put("promotedToVlmRegions", Integer.toString(
                content.promotedToVlmRegions()));
        List<PdfRepeatedMarginEvidence> repeatedMargins = repeatedMarginPolicy.detect(
                nativeEvidence, peerNativeEvidence(workspace, pageNumber));
        analysis = repeatedMarginPolicy.apply(analysis, repeatedMargins);
        captureRepeatedMargins(trace, repeatedMargins);
        List<PdfProminentTextCandidate> prominentCandidates =
                prominentTextDetector.detect(coverageEvidence, analysis, repeatedMargins);
        captureProminentCandidates(trace, prominentCandidates);
        ContentAnalysisResult diagnosticBase = primary;
        if (!prominentCandidates.isEmpty()) {
            stages.report("PROMINENT_TEXT_RECOVERY", 0.80,
                    "Clasificando texto destacado omitido de la pagina "
                            + pageNumber + ".");
            PdfSemanticRecoveryPlan prominentPlan =
                    prominentTextDetector.recoveryPlan(prominentCandidates);
            RecoveryRound prominentRecovery = runTargetedRecovery(workspace, rendered,
                    temporaryDirectory, pageNumber, previous, token, priority,
                    stages, prominentPlan);
            validateProminentRecovery(prominentCandidates,
                    prominentRecovery.analysis(), prominentRecovery.rawOutput());
            analysis = recoveryMerger.merge(analysis,
                    prominentRecovery.analysis(), true);
            analysis = repeatedMarginPolicy.apply(analysis, repeatedMargins);
            diagnosticBase = withProminentTextRecoveryDiagnostics(
                    primary, prominentPlan, prominentRecovery);
            trace.put("prominentTextRecovery.parser",
                    analysisSummary(prominentRecovery.analysis()));
            trace.put("prominentTextRecovery.raw", prominentRecovery.rawOutput());
            LOGGER.info("pdf.semantic.prominent-text-recovery page={} candidates={} "
                            + "durationMs={} promptTokens={} outputTokens={}",
                    pageNumber, prominentCandidates.size(),
                    prominentRecovery.durationMs(), prominentRecovery.promptTokens(),
                    prominentRecovery.outputTokens());
        }
        PdfSemanticCoverageResult coverage = coverageEvidenceExtractor == null
                ? new PdfSemanticCoverageResult(
                PdfSemanticCoverageStatus.ACCEPTED, false, 0.0,
                List.of("coverage-validation-not-configured"), List.of())
                : coverageValidator.validate(coverageEvidence, coverageQuality,
                analysis, false);
        captureCoverage(trace, "primary.coverage", coverage);
        captureCoverageAudit(trace, "primary.coverage", coverageEvidence, analysis);
        if (coverage.accepted()) {
            return new SemanticInference(withTrace(withCoverageDiagnostics(
                    diagnosticBase, coverage, false, ""), trace),
                    withCoverageUncertainties(analysis, coverage), coverage);
        }
        if (coverage.status() == PdfSemanticCoverageStatus.REJECTED) {
            throw new PdfSemanticCoverageException(
                    "La interpretacion primaria fue rechazada por cobertura.",
                    coverage, diagnosticBase.structuredJson(), trace);
        }

        stages.report("COVERAGE_VERIFICATION", 0.82,
                "Buscando contenido significativo omitido en la pagina "
                        + pageNumber + ".");
        String verifierContext = verifierContext(analysis, coverage);
        boolean broadCoverageRepair = coverage.reliableNativeText()
                && (coverage.coveredTokenRatio() < 0.45
                || analysis.elements().size() <= 2
                && coverage.missingEvidence().size() >= 6);
        ContentAnalysisRequest verificationRequest = withRetryAuthority(semanticRequest(
                pageImage, pageNumber, previous, priority,
                verificationInstruction(coverage.reliableNativeText(),
                        broadCoverageRepair),
                verifierContext, "",
                // P2 proved that the verifier can legitimately become the
                // recovery carrier when a short primary omitted most of a
                // page. 8K still has ample measured headroom; 1000 cut the
                // valid Block V1 stream before DONE.
                "coverage-verification", "1800"), retryAllowed);
        ContentAnalysisResult verification;
        try {
            verification = media.analyzeContent(null, verificationRequest, context);
        } catch (EngineExecutionException failure) {
            java.util.LinkedHashMap<String, String> diagnostic =
                    new java.util.LinkedHashMap<>(failure.diagnostics());
            diagnostic.put("precedingPrimaryRaw", primary.structuredJson());
            diagnostic.put("precedingPrimaryDoneReason",
                    primary.diagnostics().getOrDefault("doneReason", "unknown"));
            diagnostic.put("precedingPrimaryPromptTokens",
                    primary.diagnostics().getOrDefault("promptTokens", "0"));
            diagnostic.put("precedingPrimaryOutputTokens",
                    primary.diagnostics().getOrDefault("outputTokens", "0"));
            diagnostic.put("failureStage", "coverage-verification");
            throw new EngineExecutionException(failure.code(), failure.getMessage(),
                    diagnostic, failure);
        }
        captureStage(trace, "verifier", verification);
        token.throwIfCancelled();
        PdfSemanticPageAnalysis missing = withProvenance(parseWithDiagnostics(
                verification, "coverage-verification"), "verifier", "");
        trace.put("verifier.parser", analysisSummary(missing));
        PdfSemanticPageAnalysis merged = recoveryMerger.merge(
                analysis, missing, false);
        merged = repeatedMarginPolicy.apply(merged, repeatedMargins);
        PdfSemanticCoverageResult verified = coverageValidator.validate(
                coverageEvidence, coverageQuality, merged, true);
        captureCoverage(trace, "verifier.coverage", verified);
        captureCoverageAudit(trace, "verifier.coverage", coverageEvidence, merged);
        if (verified.accepted()) {
            ContentAnalysisResult combined = withCoverageDiagnostics(
                    diagnosticBase, verified, true, verification.structuredJson());
            return new SemanticInference(withTrace(combined, trace),
                    withCoverageUncertainties(merged, verified), verified);
        }

        PdfSemanticRecoveryPlan recoveryPlan = recoveryPlanner.plan(
                merged, verified, coverageEvidence, coverageQuality);
        trace.put("recovery.plan", recoveryPlanSummary(recoveryPlan));
        if (recoveryPlan.empty()) {
            throw new PdfSemanticCoverageException(
                    "La pagina sigue incompleta y no existe una ROI focal justificable: "
                    + String.join(", ", verified.reasons()), verified,
                    rawAttempts(primary.structuredJson(),
                            verification.structuredJson()), trace);
        }
        stages.report("TARGETED_RECOVERY", 0.88,
                "Recuperando contenido focalizado de la pagina " + pageNumber
                        + " en " + recoveryPlan.rois().size() + " region(es).");
        RecoveryRound recovery = runTargetedRecovery(workspace, rendered,
                temporaryDirectory, pageNumber, previous, token, priority,
                stages, recoveryPlan);
        trace.put("recovery.raw", recovery.rawOutput());
        trace.put("recovery.parser", analysisSummary(recovery.analysis()));
        PdfSemanticPageAnalysis recovered = recoveryMerger.merge(
                merged, recovery.analysis(), true);
        recovered = repeatedMarginPolicy.apply(recovered, repeatedMargins);
        PdfSemanticCoverageResult finalCoverage = coverageValidator.validate(
                coverageEvidence, coverageQuality, recovered, true);
        captureCoverage(trace, "final.coverage", finalCoverage);
        captureCoverageAudit(trace, "final.coverage", coverageEvidence, recovered);
        if (!finalCoverage.accepted()) {
            throw new PdfSemanticCoverageException(
                    "La pagina sigue incompleta despues de la unica ronda focalizada: "
                            + String.join(", ", finalCoverage.reasons()),
                    finalCoverage, rawAttempts(primary.structuredJson(),
                    verification.structuredJson())
                    + "\n--- TARGETED RECOVERY ---\n" + recovery.rawOutput(), trace);
        }
        ContentAnalysisResult combined = withRecoveryDiagnostics(
                withCoverageDiagnostics(diagnosticBase, finalCoverage, true,
                        verification.structuredJson()), recoveryPlan, recovery);
        LOGGER.info("pdf.semantic.recovery page={} rois={} reasons={} durationMs={} "
                        + "promptTokens={} outputTokens={}",
                pageNumber, recoveryPlan.rois().size(),
                recoveryPlan.rois().stream().map(roi -> roi.reason().name())
                        .distinct().toList(),
                recovery.durationMs(), recovery.promptTokens(),
                recovery.outputTokens());
        return new SemanticInference(withTrace(combined, trace),
                withCoverageUncertainties(recovered, finalCoverage), finalCoverage);
    }

    private RecoveryRound runTargetedRecovery(
            PreparedPdfWorkspaceRef workspace,
            PdfPageRenderResult rendered,
            Path temporaryDirectory,
            int pageNumber,
            PreparedPdfPage previous,
            PdfPreparationCancellationToken token,
            PdfPreparationPriority priority,
            ProgressSink stages,
            PdfSemanticRecoveryPlan plan)
            throws IOException, InterruptedException {
        ArrayList<PdfSemanticPageAnalysis.Element> recovered = new ArrayList<>();
        ArrayList<String> rawOutputs = new ArrayList<>();
        long durationMs = 0L;
        long promptTokens = 0L;
        long outputTokens = 0L;
        int index = 0;
        for (PdfSemanticRecoveryRoi roi : plan.rois()) {
            token.throwIfCancelled();
            index++;
            Path cropPath = temporaryDirectory.resolve("recovery-" + index + ".png");
            PdfPageRenderResult crop = null;
            try {
                crop = renderRecoveryCrop(workspace, rendered, roi, token,
                        stages, index, plan.rois().size());
                if (!ImageIO.write(crop.image(), "png", cropPath.toFile())) {
                    throw new IOException("No se pudo materializar el crop de recovery.");
                }
                ContentAnalysisRequest request = targetedRecoveryRequest(
                        cropPath, pageNumber, previous, priority, roi, index);
                ExecutionContext context = new ExecutionContext(
                        "semantic-pdf-recovery-" + pageNumber + "-" + index,
                        token::cancelled, stages, null, null);
                ContentAnalysisResult result = media.analyzeContent(
                        null, request, context);
                token.throwIfCancelled();
                PdfSemanticPageAnalysis parsed = parseWithDiagnostics(
                        result, "targeted-recovery");
                PdfSemanticPageAnalysis remapped = remapRecovery(
                        withProvenance(parsed, "targeted-recovery",
                                roi.reason().name()), roi);
                recovered.addAll(remapped.elements());
                rawOutputs.add("ROI " + index + " " + roi.reason().name()
                        + "\n" + result.structuredJson());
                durationMs += millis(result.diagnostics(), "durationMs", 0L);
                promptTokens += millis(result.diagnostics(), "promptTokens", 0L);
                outputTokens += millis(result.diagnostics(), "outputTokens", 0L);
            } finally {
                if (crop != null) crop.image().flush();
                // The outer request cleanup owns the complete managed directory.
            }
        }
        PdfSemanticPageAnalysis analysis = new PdfSemanticPageAnalysis(
                pageNumber, previous == null ? "und"
                : previous.analysisProfile().language().effectiveLanguage(),
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole.UNKNOWN,
                recovered, 0.0, List.of());
        return new RecoveryRound(analysis, String.join(
                "\n--- NEXT ROI ---\n", rawOutputs), durationMs,
                promptTokens, outputTokens);
    }

    private static ContentAnalysisRequest withRetryAuthority(
            ContentAnalysisRequest request, boolean retryAllowed) {
        LinkedHashMap<String, String> options =
                new LinkedHashMap<>(request.options());
        options.put("retryAllowed", Boolean.toString(retryAllowed));
        return new ContentAnalysisRequest(request.operation(), request.visualInputs(),
                request.instruction(), request.nearbyContext(), request.language(),
                request.responseSchema(), options);
    }

    private PdfPageRenderResult renderRecoveryCrop(
            PreparedPdfWorkspaceRef workspace,
            PdfPageRenderResult rendered,
            PdfSemanticRecoveryRoi roi,
            PdfPreparationCancellationToken cancellation,
            ProgressSink progress,
            int index,
            int total) throws IOException, InterruptedException {
        progress.report("RECOVERY_CROP", 0.88 + index * 0.01,
                "Preparando region focal " + index + " de " + total + ".");
        // This method runs inside withContentAnalysisBatch, whose global lease
        // already owns CPU_HEAVY/QWEN. Reacquiring CPU_HEAVY here deadlocks a
        // conservative capacity-1 scheduler and would create no new authority.
        cancellation.throwIfCancelled();
        PdfSemanticPageAnalysis.NormalizedBox box = roi.box();
        try {
            return renderEngine.renderCrop(new PdfCropRenderRequest(
                    workspace.sourcePath(), rendered.pageNumber(),
                    points(box.xMin(), rendered.pageWidthPoints()),
                    points(box.yMin(), rendered.pageHeightPoints()),
                    points(box.xMax(), rendered.pageWidthPoints()),
                    points(box.yMax(), rendered.pageHeightPoints()),
                    0.0, 300, MAX_PIXEL_COUNT, Color.WHITE, true));
        } catch (PdfRenderException failure) {
            throw new IOException("No se pudo rasterizar la ROI de recovery.", failure);
        }
    }

    private ContentAnalysisRequest targetedRecoveryRequest(
            Path cropPath,
            int pageNumber,
            PreparedPdfPage previous,
            PdfPreparationPriority priority,
            PdfSemanticRecoveryRoi roi,
            int index) {
        String evidence = roi.missingEvidence().isEmpty() ? "(sin texto nativo fiable)"
                : roi.missingEvidence().stream().map(value -> "- " + compact(value, 300))
                .collect(java.util.stream.Collectors.joining("\n"));
        String instruction = targetedRecoveryInstruction(roi);
        String context = "Pagina " + pageNumber + ", ROI focal " + index
                + ", motivo " + roi.reason().name() + ".\n"
                + "EVIDENCIA QUE DEBE QUEDAR CUBIERTA:\n" + evidence;
        String outputBudget = roi.reason()
                == PdfSemanticRecoveryReason.DENSE_TABLE_INCOMPLETE
                ? "1800" : roi.reason()
                == PdfSemanticRecoveryReason.PROMINENT_TEXT_UNCOVERED
                ? "300" : "1000";
        return new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                List.of(new AnalysisVisualInput(cropPath,
                        "targeted-recovery-roi", "image/png")),
                instruction, context,
                previous == null ? "und"
                        : previous.analysisProfile().language().effectiveLanguage(),
                "", Map.ofEntries(
                Map.entry("maxOutputTokens", outputBudget),
                Map.entry("contextWindowTokens", Integer.toString(vlmProfile.contextTokens())),
                Map.entry("batchSize", Integer.toString(vlmProfile.batchSize())),
                Map.entry("model", vlmProfile.model()),
                Map.entry("kvCacheType", vlmProfile.kvCacheType()),
                Map.entry("flashAttention", Boolean.toString(vlmProfile.flashAttention())),
                Map.entry("modelHostMib", Long.toString(vlmProfile.modelHostMib())),
                Map.entry("modelVramMib", Long.toString(vlmProfile.modelVramMib())),
                Map.entry("marginalHostMib", Long.toString(vlmProfile.marginalHostMib())),
                Map.entry("marginalVramMib", Long.toString(vlmProfile.marginalVramMib())),
                Map.entry("responseProtocol", TRANSPORT_VERSION),
                Map.entry("computePriority", computePriority(priority).name()),
                Map.entry("semanticTransportVersion", TRANSPORT_VERSION),
                Map.entry("semanticPass", "targeted-recovery"),
                Map.entry("recoveryReason", roi.reason().name()),
                Map.entry("pageNumber", Integer.toString(pageNumber)),
                Map.entry("promptVersion", PROMPT_VERSION)));
    }

    private static String targetedRecoveryInstruction(PdfSemanticRecoveryRoi roi) {
        if (roi.reason() == PdfSemanticRecoveryReason.VISUAL_REGION_INCOMPLETE) {
            return """
                    MIRA SOLO EL RECORTE DE LA FIGURA. Debes entregar dos contenidos distintos:
                    SOURCE = transcripcion literal de rotulos, ejes, leyenda, cifras y formulas que
                    realmente aparecen en el recorte.
                    SPEECH = explicacion semantica de que muestra la figura y de las relaciones
                    importantes: tendencias, comparaciones, posiciones, conexiones o cambios.
                    SPEECH no puede limitarse a repetir SOURCE ni quedar vacio. No inventes datos.

                    Devuelve exactamente un bloque IMAGE con esta envoltura, sustituyendo las dos
                    lineas descriptivas por contenido real observado:
                    PAGE|V1|es|CONTENT
                    BEGIN|IMAGE|0|0|1000|1000|X|98
                    SOURCE
                    rotulos reales observados en el recorte
                    SPEECH
                    explicacion real de la figura y sus relaciones visuales
                    END
                    DONE

                    SOURCE debe ser la linea inmediatamente posterior a BEGIN. SPEECH debe ser una
                    linea exacta entre el contenido SOURCE y su explicacion. No emitas END antes de
                    SPEECH. DONE es la ultima linea. No uses Markdown, JSON, placeholders ni texto
                    fuera de Block V1. Las bbox son relativas a este recorte en 0..1000.
                    """;
        }
        String task = switch (roi.reason()) {
            case DENSE_TABLE_INCOMPLETE -> """
                    Transcribe la TABLE completa de este crop. SOURCE debe contener TODAS las
                    celdas visibles, fila por fila, separando celdas con " ; ". No resumas, no
                    expliques y no sustituyas celdas por una conclusion. Devuelve una sola TABLE.
                    """;
            case MULTICOLUMN_GAP_OR_ORDER -> """
                    Lee solo la columna o bloque visible de este crop. Conserva cada parrafo sin
                    mezclar columnas ni repetir contenido ajeno al crop.
                    """;
            case TEXT_COVERAGE_GAP -> """
                    Transcribe solo los bloques significativos visibles de este crop que cubren la
                    evidencia indicada. No reescribas ni resumas.
                    """;
            case PROMINENT_TEXT_UNCOVERED -> """
                    Clasifica y transcribe exactamente el unico bloque textual destacado de este
                    crop. Decide su tipo SOLO por su funcion visual y semantica observada. Los
                    tipos permitidos son TITLE, HEADING, SUBHEADING, PARAGRAPH, SIDEBAR o CAPTION.
                    La evidencia textual solo confirma que el bloque existe: no determina su tipo.
                    SOURCE debe conservar literalmente el texto visible. No emitas SPEECH.
                    Devuelve exactamente una region y una bbox ajustada al bloque, no al crop entero.
                    En BEGIN el septimo campo debe ser la letra N, nunca 1, true ni una palabra.
                    El octavo campo debe ser un entero de 0 a 100, por ejemplo 98.
                    """;
            case SEGMENTATION_REFINEMENT -> """
                    Divide este crop solamente en unidades semantico-visuales independientes
                    de lectura. Devuelve tipo, bbox local y orden visual mediante bloques V1.
                    No describas, no resumas y no emitas SPEECH. Conserva un parrafo visual
                    completo como PARAGRAPH, una formula display como MATH, una figura como
                    IMAGE y su caption como CAPTION. No segmentes por linea ni por oracion.
                    Debes devolver al menos dos regiones hoja si realmente hay unidades
                    independientes.
                    """;
            case VISUAL_REGION_INCOMPLETE -> throw new IllegalStateException(
                    "visual recovery is handled by its dedicated contract");
            case UNKNOWN -> """
                    Recupera literalmente solo el contenido significativo visible de este crop.
                    """;
        };
        String expectedType = roi.expectedType() == null
                ? PdfRegionType.PARAGRAPH.name() : roi.expectedType().name();
        String envelope;
        envelope = roi.reason() == PdfSemanticRecoveryReason.SEGMENTATION_REFINEMENT
                ? """
                Empieza con PAGE|V1|es|CONTENT. Por cada unidad emite
                BEGIN|tipo|xMin|yMin|xMax|yMax|N|confianza, luego SOURCE, el texto visible
                de esa unidad y END. Termina con DONE. Las bbox son locales al crop.
                """
                : roi.reason() == PdfSemanticRecoveryReason.PROMINENT_TEXT_UNCOVERED
                ? """
                Empieza con PAGE|V1|es|CONTENT. Elige UNA de estas seis formas exactas de BEGIN,
                conservando literalmente el campo N y sustituyendo solo los cuatro enteros bbox:
                BEGIN|TITLE|80|100|920|900|N|98
                BEGIN|HEADING|80|100|920|900|N|98
                BEGIN|SUBHEADING|80|100|920|900|N|98
                BEGIN|PARAGRAPH|80|100|920|900|N|98
                BEGIN|SIDEBAR|80|100|920|900|N|98
                BEGIN|CAPTION|80|100|920|900|N|98
                Emite solo la forma elegida, no las seis. Luego continua exactamente con:
                SOURCE
                texto literal visible
                END
                DONE
                Usa bbox numerica local ajustada al bloque. No copies nombres de campos.
                """
                : """
                Empieza exactamente con estas tres lineas:
                PAGE|V1|es|CONTENT
                BEGIN|%s|0|0|1000|1000|N|98
                SOURCE
                Despues de SOURCE transcribe TODO el contenido real visible que cubra la
                evidencia indicada. No te detengas tras la primera frase. Termina exactamente
                con estas dos lineas:
                END
                DONE
                """.formatted(expectedType);
        return task + """

                Las bbox de la respuesta son relativas a ESTE CROP en 0..1000; Java las
                transformara a la pagina. Responde solo con el protocolo V1 completo. La primera
                linea debe ser PAGE|V1|es|CONTENT, cada BEGIN debe tener ocho campos con bbox y
                confianza numericos reales, SOURCE debe aparecer inmediatamente despues de BEGIN
                como linea exacta, sin dos puntos ni contenido adicional, y DONE debe ser la
                ultima linea.
                """ + envelope + """
                No emitas BEGIN solo. No uses Markdown, JSON, placeholders ni texto fuera del
                protocolo. SPEECH solo corresponde a IMAGE o MATH, debe ir despues de SOURCE y
                debe aparecer como linea exacta SPEECH, nunca como SPEECH: ni SPEECH seguido de
                texto en la misma linea.
                """;
    }

    private static PdfSemanticPageAnalysis remapRecovery(
            PdfSemanticPageAnalysis analysis,
            PdfSemanticRecoveryRoi roi) {
        PdfSemanticPageAnalysis.NormalizedBox crop = roi.box();
        List<PdfSemanticPageAnalysis.Element> elements = analysis.elements().stream()
                .map(element -> new PdfSemanticPageAnalysis.Element(
                        element.responseId(), element.readingOrder(), element.type(),
                        remapBox(element.box(), crop), element.sourceText(),
                        element.narrationText(), element.narratability(),
                        element.confidence(), element.uncertainties(),
                        element.attributes())).toList();
        return new PdfSemanticPageAnalysis(analysis.reportedPageNumber(),
                analysis.language(), analysis.pageRole(), elements,
                analysis.confidence(), analysis.uncertainties());
    }

    private static PdfSemanticPageAnalysis.NormalizedBox remapBox(
            PdfSemanticPageAnalysis.NormalizedBox local,
            PdfSemanticPageAnalysis.NormalizedBox crop) {
        double width = crop.xMax() - crop.xMin();
        double height = crop.yMax() - crop.yMin();
        return new PdfSemanticPageAnalysis.NormalizedBox(
                crop.xMin() + local.xMin() / 1000.0 * width,
                crop.yMin() + local.yMin() / 1000.0 * height,
                crop.xMin() + local.xMax() / 1000.0 * width,
                crop.yMin() + local.yMax() / 1000.0 * height).clamped();
    }

    private static PdfSemanticPageAnalysis withProvenance(
            PdfSemanticPageAnalysis analysis, String pass, String reason) {
        List<PdfSemanticPageAnalysis.Element> elements = analysis.elements().stream()
                .map(element -> {
                    LinkedHashMap<String, String> attributes =
                            new LinkedHashMap<>(element.attributes());
                    attributes.put("semanticPass", pass);
                    attributes.putIfAbsent("layoutAuthority",
                            "primary".equals(pass) ? "primary" : "inherited");
                    attributes.putIfAbsent("regionRole", "LEAF");
                    attributes.putIfAbsent("playbackTarget", "true");
                    attributes.putIfAbsent("contentRoute",
                            PdfRegionLayoutSemantics.route(element.type(),
                                    element.sourceText()).name());
                    if (reason != null && !reason.isBlank()) {
                        attributes.put("recoveryReason", reason);
                    }
                    return new PdfSemanticPageAnalysis.Element(
                            element.responseId(), element.readingOrder(), element.type(),
                            element.box(), element.sourceText(), element.narrationText(),
                            element.narratability(), element.confidence(),
                            element.uncertainties(), attributes);
                }).toList();
        return new PdfSemanticPageAnalysis(analysis.reportedPageNumber(),
                analysis.language(), analysis.pageRole(), elements,
                analysis.confidence(), analysis.uncertainties());
    }

    private static ContentAnalysisResult withRecoveryDiagnostics(
            ContentAnalysisResult base,
            PdfSemanticRecoveryPlan plan,
            RecoveryRound recovery) {
        LinkedHashMap<String, String> diagnostics =
                new LinkedHashMap<>(base.diagnostics());
        diagnostics.put("semanticRecoveryUsed", "true");
        diagnostics.put("semanticRecoveryRoiCount",
                Integer.toString(plan.rois().size()));
        diagnostics.put("semanticRecoveryReasons", plan.rois().stream()
                .map(value -> value.reason().name()).distinct()
                .collect(java.util.stream.Collectors.joining(",")));
        diagnostics.put("semanticRecoveryDurationMs",
                Long.toString(recovery.durationMs()));
        diagnostics.put("semanticRecoveryPromptTokens",
                Long.toString(recovery.promptTokens()));
        diagnostics.put("semanticRecoveryOutputTokens",
                Long.toString(recovery.outputTokens()));
        diagnostics.put("semanticRecoveryRawOutput", recovery.rawOutput());
        return new ContentAnalysisResult(base.text(), base.structuredJson(),
                base.confidence(), base.uncertainties(), diagnostics);
    }

    private static ContentAnalysisResult withProminentTextRecoveryDiagnostics(
            ContentAnalysisResult base,
            PdfSemanticRecoveryPlan plan,
            RecoveryRound recovery) {
        LinkedHashMap<String, String> diagnostics =
                new LinkedHashMap<>(base.diagnostics());
        diagnostics.put("prominentTextRecoveryUsed", "true");
        diagnostics.put("prominentTextRecoveryRoiCount",
                Integer.toString(plan.rois().size()));
        diagnostics.put("prominentTextRecoveryDurationMs",
                Long.toString(recovery.durationMs()));
        diagnostics.put("prominentTextRecoveryPromptTokens",
                Long.toString(recovery.promptTokens()));
        diagnostics.put("prominentTextRecoveryOutputTokens",
                Long.toString(recovery.outputTokens()));
        diagnostics.put("prominentTextRecoveryRawOutput", recovery.rawOutput());
        return new ContentAnalysisResult(base.text(), base.structuredJson(),
                base.confidence(), base.uncertainties(), diagnostics);
    }

    private record RecoveryRound(PdfSemanticPageAnalysis analysis,
                                 String rawOutput,
                                 long durationMs,
                                 long promptTokens,
                                 long outputTokens) { }

    private ContentAnalysisRequest semanticRequest(
            Path pageImage, int pageNumber, PreparedPdfPage previous,
            PdfPreparationPriority priority, String instruction,
            String nearbyContext, String schema, String pass,
            String outputTokens) {
        return new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                List.of(new AnalysisVisualInput(
                        pageImage, "complete-page", "image/png")),
                instruction, nearbyContext,
                previous == null ? "und"
                        : previous.analysisProfile().language().effectiveLanguage(),
                schema,
                Map.ofEntries(
                        Map.entry("maxOutputTokens", outputTokens),
                        Map.entry("contextWindowTokens", Integer.toString(vlmProfile.contextTokens())),
                        Map.entry("batchSize", Integer.toString(vlmProfile.batchSize())),
                        Map.entry("model", vlmProfile.model()),
                        Map.entry("kvCacheType", vlmProfile.kvCacheType()),
                        Map.entry("flashAttention", Boolean.toString(vlmProfile.flashAttention())),
                        Map.entry("modelHostMib", Long.toString(vlmProfile.modelHostMib())),
                        Map.entry("modelVramMib", Long.toString(vlmProfile.modelVramMib())),
                        Map.entry("marginalHostMib", Long.toString(vlmProfile.marginalHostMib())),
                        Map.entry("marginalVramMib", Long.toString(vlmProfile.marginalVramMib())),
                        Map.entry("responseProtocol", TRANSPORT_VERSION),
                        Map.entry("computePriority", computePriority(priority).name()),
                        Map.entry("semanticTransportVersion", TRANSPORT_VERSION),
                        Map.entry("semanticPass", pass),
                        Map.entry("semanticStrategyVersion", "coverage-verification".equals(pass)
                                ? VERIFIER_STRATEGY_VERSION : PROMPT_VERSION),
                        Map.entry("pageNumber", Integer.toString(pageNumber)),
                        Map.entry("promptVersion", PROMPT_VERSION)));
    }

    private PdfSemanticPageAnalysis parseWithDiagnostics(
            ContentAnalysisResult result, String stage)
            throws IOException {
        try {
            return responseParser.parse(result.structuredJson());
        } catch (PdfSemanticProtocolException failure) {
            java.util.LinkedHashMap<String, String> diagnostic =
                    new java.util.LinkedHashMap<>(result.diagnostics());
            diagnostic.put("failureStage", stage);
            diagnostic.put("rawOutput", result.structuredJson());
            throw new PdfSemanticProtocolException(failure.kind(),
                    failure.getMessage(), diagnostic, failure);
        }
    }

    private PdfTextLayer coverageEvidence(PreparedPdfWorkspaceRef workspace,
                                          int pageNumber) {
        if (coverageEvidenceExtractor == null) {
            return new PdfTextLayer(pageNumber,
                    PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of());
        }
        try {
            return coverageEvidenceExtractor.extract(
                    workspace.sourcePath(), pageNumber);
        } catch (RuntimeException failure) {
            LOGGER.warn("pdf.semantic.native-evidence-failure page={} reason={}",
                    pageNumber, failure.toString());
            return new PdfTextLayer(pageNumber,
                    PdfTextLayerOrigin.UNAVAILABLE, List.of(),
                    List.of("pdfbox-coverage-evidence-failed"));
        }
    }

    private List<PdfTextLayer> peerNativeEvidence(
            PreparedPdfWorkspaceRef workspace, int pageNumber) {
        if (coverageEvidenceExtractor == null) return List.of();
        ArrayList<PdfTextLayer> peers = new ArrayList<>();
        if (pageNumber > 1) {
            addPeerNativeEvidence(peers, workspace, pageNumber - 1);
        }
        addPeerNativeEvidence(peers, workspace, pageNumber + 1);
        return List.copyOf(peers);
    }

    private void addPeerNativeEvidence(List<PdfTextLayer> peers,
                                       PreparedPdfWorkspaceRef workspace,
                                       int pageNumber) {
        try {
            PdfTextLayer peer = coverageEvidenceExtractor.extract(
                    workspace.sourcePath(), pageNumber);
            if (peer != null && peer.available()) peers.add(peer);
        } catch (RuntimeException failure) {
            LOGGER.debug("pdf.semantic.peer-native-evidence-unavailable page={} reason={}",
                    pageNumber, failure.toString());
        }
    }

    private static void validateProminentRecovery(
            List<PdfProminentTextCandidate> candidates,
            PdfSemanticPageAnalysis recovered,
            String rawOutput) throws PdfSemanticCoverageException {
        Set<PdfRegionType> allowed = Set.of(PdfRegionType.TITLE,
                PdfRegionType.HEADING, PdfRegionType.SUBHEADING,
                PdfRegionType.PARAGRAPH, PdfRegionType.SIDEBAR,
                PdfRegionType.CAPTION);
        ArrayList<String> missing = new ArrayList<>();
        for (PdfProminentTextCandidate candidate : candidates) {
            PdfSemanticPageAnalysis.NormalizedBox evidence = normalized(
                    candidate.region());
            boolean matched = recovered.elements().stream()
                    .filter(element -> allowed.contains(element.type()))
                    .anyMatch(element -> PdfRepeatedMarginPolicy.textSimilarity(
                                    candidate.text(), element.sourceText()) >= 0.60
                            && PdfSemanticRecoveryMerger.overlap(
                                    evidence, element.box()) >= 0.35);
            if (!matched) missing.add(candidate.text());
        }
        if (missing.isEmpty()) return;
        PdfSemanticCoverageResult result = new PdfSemanticCoverageResult(
                PdfSemanticCoverageStatus.REJECTED, true, 0.0,
                List.of("prominent-text-recovery-invalid"), missing);
        throw new PdfSemanticCoverageException(
                "La recuperacion focal no clasifico de forma valida todo el texto destacado.",
                result, rawOutput, Map.of(
                "failureStage", "PROMINENT_TEXT_RECOVERY",
                "missingProminentText", String.join(" | ", missing)));
    }

    private static PdfSemanticPageAnalysis.NormalizedBox normalized(
            PdfPageRegion box) {
        return new PdfSemanticPageAnalysis.NormalizedBox(
                box.xMinPoints() / box.pageWidthPoints() * 1000.0,
                box.yMinPoints() / box.pageHeightPoints() * 1000.0,
                box.xMaxPoints() / box.pageWidthPoints() * 1000.0,
                box.yMaxPoints() / box.pageHeightPoints() * 1000.0);
    }

    private static String verificationInstruction(boolean nativeEvidence,
                                                  boolean broadCoverageRepair) {
        return """
                TAREA: verifica la misma pagina contra las regiones detectadas. %s
                No repitas regiones correctas. Conserva texto, formulas, rotulos y todas las celdas
                literalmente. Una tabla visible se devuelve como una sola TABLE con filas por salto
                y celdas separadas por " ; ". CODE no se convierte en TABLE por estar alineado.
                Revisa pageRole. %s

                SOURCE y SPEECH nombran LINEAS DE CONTROL del protocolo; nunca los suprimas ni los
                sustituyas por el contenido. El contenido siempre va en las lineas posteriores.
                Para IMAGE: SOURCE son los rotulos visibles y SPEECH explica relaciones visuales;
                SPEECH no debe repetir solamente los rotulos.

                SALIDA SI NO HAY CORRECCIONES (exactamente dos lineas):
                PAGE|V1|es|CONTENT
                DONE

                SALIDA SI HAY UNA CORRECCION (copia esta envoltura completa):
                PAGE|V1|es|CONTENT
                BEGIN|PARAGRAPH|0|0|1000|1000|N|98
                SOURCE
                texto visible real de la region
                END
                DONE

                Para IMAGE usa esta envoltura completa:
                BEGIN|IMAGE|0|0|1000|1000|X|98
                SOURCE
                rotulos visibles reales
                SPEECH
                explicacion real de la figura y relaciones entre sus elementos
                END

                REGLAS FINALES OBLIGATORIAS:
                - Responde solo Block V1; no agregues prosa, Markdown, JSON ni listas explicativas.
                - Cada BEGIN tiene 8 campos: BEGIN|tipo|xMin|yMin|xMax|yMax|N/X/U|confianza.
                - Cada BEGIN va seguido INMEDIATAMENTE por una linea que contiene solo SOURCE.
                - Esta prohibido abreviar un bloque como BEGIN, contenido, END.
                - SOURCE nunca es opcional, incluso al devolver solamente correcciones.
                - SPEECH, cuando se use, es una linea exacta posterior al contenido de SOURCE.
                - END cierra cada bloque y DONE es siempre la ultima linea.
                - No termines tras la primera correccion: recorre la pagina hasta el margen inferior
                  y emite TODOS los bloques omitidos necesarios para cubrir la tarea.
                Antes de responder, comprueba mecanicamente que la linea posterior a CADA BEGIN
                sea SOURCE. Si no lo es, corrige la salida antes de enviarla.
                """.formatted(broadCoverageRepair
                ? "La lectura anterior es severamente incompleta: recorre visualmente de arriba abajo y devuelve TODOS los bloques ausentes, no solo el primero."
                : "Devuelve solamente regiones visibles que falten o una region completa cuando necesite correccion.",
                nativeEvidence
                ? "Los fragmentos de evidencia nativa marcados como ausentes son pistas de cobertura, no autoridad semantica."
                : "No existe texto nativo fiable: inspecciona visualmente titulos, prosa, tablas, formulas, figuras y codigo omitidos.");
    }

    private static String verifierContext(PdfSemanticPageAnalysis analysis,
                                          PdfSemanticCoverageResult coverage) {
        String detected = analysis.elements().stream().limit(30)
                // Do not expose comma-packed coordinates here. The measured
                // P2 verifier copied that diagnostic notation into BEGIN and
                // thereby violated Block V1 despite the explicit grammar.
                .map(element -> element.type() + ": "
                        + compact(element.sourceText(), 180))
                .collect(java.util.stream.Collectors.joining("\n"));
        String missing = coverage.missingEvidence().stream()
                .map(value -> "- " + compact(value, 260))
                .collect(java.util.stream.Collectors.joining("\n"));
        String context = "RAZONES DE VALIDACION QUE DEBES CORREGIR:\n- "
                + coverage.reasons().stream().map(
                        AnalyzePdfPageSemanticallyUseCase::actionableReason)
                .collect(java.util.stream.Collectors.joining("\n- "))
                + "\n\nREGIONES YA DETECTADAS:\n" + detected
                + "\n\nEVIDENCIA POSIBLEMENTE AUSENTE:\n"
                + (missing.isBlank() ? "(sin evidencia textual fiable)" : missing)
                + "\n\nCOBERTURA NATIVA ACTUAL: "
                + String.format(Locale.ROOT, "%.3f", coverage.coveredTokenRatio());
        return context.substring(0, Math.min(6000, context.length()));
    }

    private static String actionableReason(String reason) {
        return switch (reason) {
            case "imageMissingVisualExplanation" ->
                    "La region IMAGE carece de speech: devuelve esa region corregida con sus rotulos en source y relaciones visuales en speech.";
            case "tableMissingVisibleCells" ->
                    "La region TABLE no demuestra conservar sus celdas: devuelve la tabla corregida con todas las filas y celdas.";
            case "suspiciousPageRole" ->
                    "pageRole parece incompatible con una pagina explicativa: inspecciona y devuelve el rol correcto.";
            case "suspiciousSparseStructuredPage" ->
                    "La pagina INDEX/CATALOG/BIBLIOGRAPHY contiene demasiadas pocas entradas: revisa ambas columnas y devuelve todas las entradas visibles omitidas.";
            case "suspiciousMathType" ->
                    "Una region MATH parece prosa ordinaria: devuelve la correccion de tipo sin omitir su texto.";
            case "nativeTextCoverageGap", "missingSignificantTextBlocks" ->
                    "La evidencia nativa indica texto significativo ausente: devuelve unicamente las regiones que lo cubren.";
            default -> reason;
        };
    }

    private static PdfSemanticPageAnalysis merge(
            PdfSemanticPageAnalysis primary,
            PdfSemanticPageAnalysis additions) {
        ArrayList<PdfSemanticPageAnalysis.Element> merged =
                new ArrayList<>(primary.elements());
        for (PdfSemanticPageAnalysis.Element addition : additions.elements()) {
            int duplicate = -1;
            for (int index = 0; index < merged.size(); index++) {
                PdfSemanticPageAnalysis.Element existing = merged.get(index);
                if (normalized(existing.sourceText()).equals(
                        normalized(addition.sourceText()))
                        || overlap(existing.box(), addition.box()) >= 0.78) {
                    duplicate = index;
                    break;
                }
            }
            if (duplicate < 0) {
                merged.add(addition);
            } else if (addition.sourceText().length()
                    > merged.get(duplicate).sourceText().length()) {
                merged.set(duplicate, addition);
            }
        }
        merged.sort(Comparator
                .comparingDouble((PdfSemanticPageAnalysis.Element value) ->
                        value.box().yMin())
                .thenComparingDouble(value -> value.box().xMin()));
        ArrayList<PdfSemanticPageAnalysis.Element> ordered = new ArrayList<>();
        for (int index = 0; index < merged.size(); index++) {
            PdfSemanticPageAnalysis.Element value = merged.get(index);
            ordered.add(new PdfSemanticPageAnalysis.Element(
                    value.responseId(), index, value.type(), value.box(),
                    value.sourceText(), value.narrationText(),
                    value.narratability(), value.confidence(),
                    value.uncertainties(), value.attributes()));
        }
        return new PdfSemanticPageAnalysis(0, primary.language(),
                additions.pageRole() == com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole.UNKNOWN
                        ? primary.pageRole() : additions.pageRole(), ordered, 0.0,
                primary.uncertainties());
    }

    private static double overlap(PdfSemanticPageAnalysis.NormalizedBox a,
                                  PdfSemanticPageAnalysis.NormalizedBox b) {
        double intersection = Math.max(0.0,
                Math.min(a.xMax(), b.xMax()) - Math.max(a.xMin(), b.xMin()))
                * Math.max(0.0,
                Math.min(a.yMax(), b.yMax()) - Math.max(a.yMin(), b.yMin()));
        double smaller = Math.min(
                (a.xMax() - a.xMin()) * (a.yMax() - a.yMin()),
                (b.xMax() - b.xMin()) * (b.yMax() - b.yMin()));
        return intersection / Math.max(1.0, smaller);
    }

    private static PdfSemanticPageAnalysis withCoverageUncertainties(
            PdfSemanticPageAnalysis analysis,
            PdfSemanticCoverageResult coverage) {
        ArrayList<String> uncertainties = new ArrayList<>(analysis.uncertainties());
        uncertainties.addAll(coverage.reasons());
        uncertainties.add("semantic-coverage-status:" + coverage.status().name());
        return new PdfSemanticPageAnalysis(analysis.reportedPageNumber(),
                analysis.language(), analysis.pageRole(), analysis.elements(),
                0.0, uncertainties);
    }

    private static ContentAnalysisResult withCoverageDiagnostics(
            ContentAnalysisResult primary,
            PdfSemanticCoverageResult coverage,
            boolean verifierUsed,
            String verifierOutput) {
        LinkedHashMap<String, String> diagnostics =
                new LinkedHashMap<>(primary.diagnostics());
        diagnostics.put("semanticCoverageStatus", coverage.status().name());
        diagnostics.put("reliableNativeText",
                Boolean.toString(coverage.reliableNativeText()));
        diagnostics.put("coveredTokenRatio",
                number(coverage.coveredTokenRatio()));
        diagnostics.put("coverageVerifierUsed", Boolean.toString(verifierUsed));
        diagnostics.put("semanticRecoveryUsed", "false");
        diagnostics.put("semanticRecoveryRoiCount", "0");
        diagnostics.put("coverageReasons", String.join(",", coverage.reasons()));
        if (verifierOutput != null && !verifierOutput.isBlank()) {
            diagnostics.put("coverageVerifierRawOutput", verifierOutput);
        }
        return new ContentAnalysisResult("", primary.structuredJson(), 0.0,
                coverage.reasons(), diagnostics);
    }

    private static void captureStage(Map<String, String> trace,
                                     String stage,
                                     ContentAnalysisResult result) {
        trace.put(stage + ".raw", Objects.toString(result.structuredJson(), ""));
        result.diagnostics().forEach((key, value) -> {
            if (!key.toLowerCase(Locale.ROOT).contains("base64")) {
                trace.put(stage + ".engine." + key, Objects.toString(value, ""));
            }
        });
    }

    private static void captureRepeatedMargins(
            Map<String, String> trace,
            List<PdfRepeatedMarginEvidence> repeated) {
        trace.put("repeatedMargin.count", Integer.toString(repeated.size()));
        trace.put("repeatedMargin.evidence", repeated.stream()
                .map(value -> value.type() + "|pages=" + value.matchingPages()
                        + "|geometry=" + number(value.geometrySimilarity())
                        + "|text=" + compact(value.text(), 160))
                .collect(java.util.stream.Collectors.joining("\n")));
    }

    private static void captureProminentCandidates(
            Map<String, String> trace,
            List<PdfProminentTextCandidate> candidates) {
        trace.put("prominentTextCandidate.count",
                Integer.toString(candidates.size()));
        trace.put("prominentTextCandidate.evidence", candidates.stream()
                .map(value -> value.evidenceId() + "|source=" + value.source()
                        + "|confidence=" + number(value.confidence())
                        + "|prominence=" + number(value.prominenceRatio())
                        + "|relativeY=" + number(value.relativeY())
                        + "|signals=" + value.signals()
                        + "|text=" + compact(value.text(), 200))
                .collect(java.util.stream.Collectors.joining("\n")));
    }

    private static void captureCoverage(Map<String, String> trace,
                                        String stage,
                                        PdfSemanticCoverageResult coverage) {
        trace.put(stage + ".status", coverage.status().name());
        trace.put(stage + ".reliableNativeText",
                Boolean.toString(coverage.reliableNativeText()));
        trace.put(stage + ".coveredTokenRatio",
                number(coverage.coveredTokenRatio()));
        trace.put(stage + ".reasons", coverage.reasons().toString());
        trace.put(stage + ".missingEvidence",
                coverage.missingEvidence().toString());
    }

    private void captureCoverageAudit(Map<String, String> trace,
                                      String stage,
                                      PdfTextLayer evidence,
                                      PdfSemanticPageAnalysis candidate) {
        String rows = coverageValidator.audit(evidence, candidate).stream()
                .filter(item -> item.significantMissingContent()
                        || item.classification()
                        != PdfSemanticCoverageEvidenceAudit.Classification
                        .COVERED_DIFFERENT_REPRESENTATION)
                .map(item -> item.evidenceId() + "|text="
                        + compact(item.text(), 240) + "|bbox="
                        + number(item.bbox().xMinPoints()) + ","
                        + number(item.bbox().yMinPoints()) + ","
                        + number(item.bbox().xMaxPoints()) + ","
                        + number(item.bbox().yMaxPoints()) + "|area="
                        + number(item.areaPoints()) + "|source=" + item.source()
                        + "|coveredBy=" + item.alreadyCoveredByRegionId()
                        + "|semanticType=" + Objects.toString(
                        item.semanticRegionType(), "") + "|overlap="
                        + number(item.overlap()) + "|textSimilarity="
                        + number(item.textSimilarity()) + "|mathematical="
                        + item.mathematical() + "|headerFooter="
                        + item.headerFooter() + "|classification="
                        + item.classification() + "|finalDecision="
                        + (item.significantMissingContent() ? "MISSING" : "EXCLUDED"))
                .collect(java.util.stream.Collectors.joining("\n"));
        trace.put(stage + ".evidenceAudit", rows.isBlank() ? "none" : rows);
        trace.put(stage + ".trueMissingContent", Long.toString(
                coverageValidator.audit(evidence, candidate).stream()
                        .filter(PdfSemanticCoverageEvidenceAudit
                                ::significantMissingContent).count()));
    }

    private static String analysisSummary(PdfSemanticPageAnalysis analysis) {
        return "pageRole=" + analysis.pageRole()
                + ";elements=" + analysis.elements().size()
                + ";types=" + analysis.elements().stream()
                .map(element -> element.type().name())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String recoveryPlanSummary(PdfSemanticRecoveryPlan plan) {
        if (plan.empty()) return "empty";
        return plan.rois().stream().map(roi -> roi.reason().name()
                        + "@" + number(roi.box().xMin()) + ","
                        + number(roi.box().yMin()) + ","
                        + number(roi.box().xMax()) + ","
                        + number(roi.box().yMax())
                        + ":" + roi.missingEvidence())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static ContentAnalysisResult withTrace(ContentAnalysisResult result,
                                                   Map<String, String> trace) {
        LinkedHashMap<String, String> diagnostics =
                new LinkedHashMap<>(result.diagnostics());
        trace.forEach((key, value) -> diagnostics.put(
                "semanticTrace." + key, Objects.toString(value, "")));
        return new ContentAnalysisResult(result.text(), result.structuredJson(),
                result.confidence(), result.uncertainties(), diagnostics);
    }

    private static String rawAttempts(String primary, String verifier) {
        return Objects.toString(primary, "")
                + (verifier == null || verifier.isBlank() ? ""
                : "\n--- COVERAGE VERIFIER ---\n" + verifier);
    }

    private static String compact(String value, int limit) {
        String safe = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        return safe.length() <= limit ? safe : safe.substring(0, limit) + "...";
    }

    private PdfTextGeometryMap rasterTextGeometry(
            PreparedPdfWorkspaceRef workspace, int pageNumber,
            Map<String, String> trace) {
        if (textGeometryOcrEngine == null) {
            trace.put("textGeometry.origin", "UNAVAILABLE");
            trace.put("textGeometry.reason", "ocr-engine-not-configured");
            return PdfTextGeometryMap.unavailable(pageNumber,
                    "OCR engine not configured");
        }
        Path cache = workspace.projectRoot().resolve("document")
                .resolve("ocr-cache");
        try {
            // Automatic page segmentation is appropriate for the single,
            // shared page pass. The OCR engine caches the resulting TSV.
            PdfOcrPageResult result = textGeometryOcrEngine.recognize(
                    new PdfOcrRequest(workspace.sourcePath(), pageNumber, 300,
                            PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                            PdfOcrRequest.DEFAULT_LANGUAGES, true, cache,
                            3, 0, null));
            PdfTextGeometryMap map = new PdfTextGeometryMap(pageNumber,
                    result.textLayer(), result.words(), "OCR_RASTER_PAGE",
                    result.warnings());
            trace.put("textGeometry.origin", map.origin());
            trace.put("textGeometry.lines", Integer.toString(
                    map.textLayer().lines().size()));
            trace.put("textGeometry.words", Integer.toString(map.words().size()));
            trace.put("textGeometry.warnings", map.warnings().toString());
            return map;
        } catch (PdfOcrException | RuntimeException failure) {
            LOGGER.warn("pdf.text-geometry.unavailable page={} reason={}",
                    pageNumber, failure.toString());
            trace.put("textGeometry.origin", "UNAVAILABLE");
            trace.put("textGeometry.reason", failure.toString());
            return PdfTextGeometryMap.unavailable(pageNumber,
                    failure.getMessage());
        }
    }

    private static PdfTextLayer coverageEvidence(
            PdfTextLayer nativeEvidence, PdfTextGeometryMap geometry) {
        if (geometry == null || !geometry.available()) return nativeEvidence;
        if (nativeEvidence == null || !nativeEvidence.available()) {
            return geometry.textLayer();
        }
        ArrayList<PdfTextLine> combined = new ArrayList<>(nativeEvidence.lines());
        Set<String> seen = new java.util.LinkedHashSet<>();
        nativeEvidence.lines().forEach(line -> seen.add(evidenceKey(line)));
        geometry.textLayer().lines().stream()
                .filter(line -> seen.add(evidenceKey(line)))
                .forEach(combined::add);
        combined.sort(Comparator.comparingDouble(line ->
                line.region().yMinPoints()));
        ArrayList<String> warnings = new ArrayList<>(nativeEvidence.warnings());
        warnings.addAll(geometry.warnings());
        return new PdfTextLayer(nativeEvidence.pageNumber(),
                PdfTextLayerOrigin.OCR_LOCAL, combined, warnings);
    }

    private static String evidenceKey(PdfTextLine line) {
        PdfPageRegion box = line.region();
        return PdfSemanticCoverageValidator.normalized(line.text()) + "|"
                + Math.round(box.xMinPoints() / 3.0) + "|"
                + Math.round(box.yMinPoints() / 3.0);
    }

    private record SemanticInference(
            ContentAnalysisResult result,
            PdfSemanticPageAnalysis analysis,
            PdfSemanticCoverageResult coverage) { }

    private PdfPageRenderResult render(PreparedPdfWorkspaceRef workspace,
                                       int pageNumber,
                                       Path pageImage,
                                       PdfPreparationCancellationToken cancellation,
                                       PdfPreparationPriority priority,
                                       ProgressSink progress,
                                       OperationDeadline deadline,
                                       Runnable leaseAcquired)
            throws IOException, InterruptedException {
        ComputeAdmissionRequest admission = new ComputeAdmissionRequest(
                "semantic-page-render-" + workspace.sourceSha256() + "-" + pageNumber,
                "semantic-page-render-" + pageNumber,
                computePriority(priority),
                ComputeWorkloadKind.DOCUMENT_PREPARATION,
                ComputeResourceDemand.of(ResourceId.CPU_HEAVY),
                cancellation::cancelled, deadline);
        progress.report("LEASE_REQUESTED", 0.07,
                "Reservando recursos para renderizar la pagina " + pageNumber + ".");
        try (var ignored = media.resourceScheduler().acquire(admission)) {
            if (leaseAcquired != null) leaseAcquired.run();
            progress.report("LEASE_ACQUIRED", 0.09,
                    "Recursos de render disponibles para la pagina " + pageNumber + ".");
            cancellation.throwIfCancelled();
            PdfPageRenderResult rendered;
            try {
                rendered = renderEngine.renderPage(new PdfPageRenderRequest(
                        workspace.sourcePath(), pageNumber, RENDER_DPI,
                        MAX_PIXEL_COUNT, Color.WHITE, true));
            } catch (PdfRenderException failure) {
                throw new IOException("No se pudo rasterizar la pagina para Qwen.", failure);
            }
            boolean published = false;
            try {
                if (!ImageIO.write(rendered.image(), "png", pageImage.toFile())) {
                    throw new IOException(
                            "No se pudo preparar el PNG para la lectura semantica.");
                }
                published = true;
                return rendered;
            } finally {
                if (!published) rendered.image().flush();
            }
        }
    }

    private PreparedPdfPage assemble(
            PreparedPdfWorkspaceRef workspace,
            PdfPageRenderResult rendered,
            PdfSemanticPageAnalysis analysis,
            ContentAnalysisResult result,
            PreparedPdfPage previous,
            long startedNanos,
            long startedCpu,
            long startedHeap) throws IOException {
        try {
            granularityValidator.validateCanonical(analysis);
        } catch (IllegalArgumentException invalidGranularity) {
            throw new IOException("La pagina semantica viola la granularidad leaf/container.",
                    invalidGranularity);
        }
        ArrayList<String> warnings = new ArrayList<>(rendered.warnings());
        warnings.addAll(analysis.uncertainties());
        warnings.addAll(result.uncertainties());
        ArrayList<PdfRegion> candidates = new ArrayList<>();
        ArrayList<PdfSemanticPageAnalysis.Element> admitted = new ArrayList<>();
        List<PdfSemanticPageAnalysis.Element> ordered = analysis.elements().stream()
                .sorted(Comparator.comparingInt(PdfSemanticPageAnalysis.Element::readingOrder)
                        .thenComparingDouble(element -> element.box().yMin())
                        .thenComparingDouble(element -> element.box().xMin()))
                .toList();
        LinkedHashMap<String, String> layoutIds = new LinkedHashMap<>();
        for (PdfSemanticPageAnalysis.Element element : ordered) {
            String layoutNodeId = element.attributes().getOrDefault(
                    "layoutNodeId", "");
            if (!layoutNodeId.isBlank() && element.box().positive()
                    && element.box().insideCanonicalRange()) {
                layoutIds.put(layoutNodeId, stableRegionId(workspace.sourceSha256(),
                        rendered.pageNumber(), element, element.box()));
            }
        }
        int order = 0;
        for (PdfSemanticPageAnalysis.Element element : ordered) {
            PdfSemanticPageAnalysis.NormalizedBox box = element.box();
            if (!box.insideCanonicalRange()) {
                throw new IOException("El parser publico un bbox fuera del rango 0..1000.");
            }
            if (!box.positive()) {
                warnings.add("Se omitio un elemento semantico con bbox vacio.");
                continue;
            }
            if (element.sourceText().isBlank()
                    && element.narrationText().isBlank()
                    && element.narratability() != PdfNarratability.NON_NARRATABLE) {
                warnings.add("Se omitio un elemento semantico sin texto ni narracion.");
                continue;
            }
            double xMin = points(box.xMin(), rendered.pageWidthPoints());
            double yMin = points(box.yMin(), rendered.pageHeightPoints());
            double xMax = points(box.xMax(), rendered.pageWidthPoints());
            double yMax = points(box.yMax(), rendered.pageHeightPoints());
            String id = stableRegionId(workspace.sourceSha256(), rendered.pageNumber(),
                    element, box);
            LinkedHashMap<String, String> attributes = new LinkedHashMap<>(
                    element.attributes());
            attributes.putIfAbsent("regionRole", "LEAF");
            attributes.putIfAbsent("playbackTarget", "true");
            attributes.putIfAbsent("contentRoute",
                    PdfRegionLayoutSemantics.route(element.type(),
                            element.sourceText()).name());
            String parentLayoutNodeId = attributes.remove("parentLayoutNodeId");
            if (parentLayoutNodeId != null && !parentLayoutNodeId.isBlank()) {
                String parentId = layoutIds.getOrDefault(parentLayoutNodeId, "");
                if (!parentId.isBlank()) attributes.put("parentId", parentId);
            }
            if (!element.responseId().isBlank()) {
                attributes.put("vlmResponseId", element.responseId());
            }
            attributes.put("semanticTransportVersion", TRANSPORT_VERSION);
            attributes.put("semanticPromptVersion", PROMPT_VERSION);
            attributes.put("renderDpi", Integer.toString(rendered.dpi()));
            attributes.put("renderWidthPixels",
                    Integer.toString(rendered.image().getWidth()));
            attributes.put("renderHeightPixels",
                    Integer.toString(rendered.image().getHeight()));
            attributes.put("bboxCoordinateSpace", "NORMALIZED_0_1000");
            attributes.put("semanticCoverageStatus",
                    result.diagnostics().getOrDefault(
                            "semanticCoverageStatus", "UNKNOWN"));
            attributes.put("modelConfidence", "not-reported");
            String mathConvention = mathSourceConvention(
                    element.type(), element.sourceText());
            if (!mathConvention.isBlank()) {
                attributes.put("mathSourceConvention", mathConvention);
            }
            ArrayList<String> reasons = new ArrayList<>(element.uncertainties());
            int columnIndex;
            try {
                columnIndex = Math.max(0, Integer.parseInt(
                        attributes.getOrDefault("columnIndex", "0")));
            } catch (NumberFormatException ignored) {
                columnIndex = 0;
            }
            PdfRegion region = new PdfRegion(
                    id, rendered.pageNumber(), xMin, yMin, xMax, yMax,
                    columnIndex, order++, element.sourceText(), element.type(),
                    effectiveNarratability(element), reasons,
                    new PdfRegionEvidence(PdfRegionOrigin.VLM_SEMANTIC,
                            0.0,
                            result.diagnostics().getOrDefault("model", "qwen-local"),
                            TRANSPORT_VERSION, SEMANTIC_SIGNATURE, PROMPT_VERSION),
                    PdfRegionOverride.empty(), attributes, 1L);
            candidates.add(region);
            admitted.add(element);
        }
        List<PdfRegion> regions = new PdfRegionReconciler().reconcile(
                previous == null ? List.of() : previous.regions(), candidates);
        LinkedHashMap<String, String> reconciledIds = new LinkedHashMap<>();
        for (int index = 0; index < Math.min(candidates.size(), regions.size()); index++) {
            reconciledIds.put(candidates.get(index).id(), regions.get(index).id());
        }
        regions = regions.stream().map(region -> {
            String parentId = region.parentId();
            String reconciledParent = reconciledIds.getOrDefault(parentId, parentId);
            if (parentId.equals(reconciledParent)) return region;
            LinkedHashMap<String, String> attributes =
                    new LinkedHashMap<>(region.attributes());
            attributes.put("parentId", reconciledParent);
            return new PdfRegion(region.id(), region.pageNumber(), region.xMin(),
                    region.yMin(), region.xMax(), region.yMax(),
                    region.columnIndex(), region.readingOrder(), region.text(),
                    region.automaticType(), region.automaticNarratability(),
                    region.reasons(), region.evidence(), region.evidenceCandidates(),
                    region.override(), attributes, region.revision());
        }).toList();
        String responseFingerprint = sha256(result.structuredJson());
        List<PdfDerivedTreatment> preserved = preserveUserTreatments(previous, regions);
        Set<String> decidedRegions = preserved.stream()
                .filter(value -> value.state() != PdfDerivedTreatmentState.DRAFT)
                .flatMap(value -> value.sourceRegionIds().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        ArrayList<PdfDerivedTreatment> treatments = new ArrayList<>(preserved);
        for (int index = 0; index < regions.size(); index++) {
            PdfRegion region = regions.get(index);
            PdfSemanticPageAnalysis.Element element = admitted.get(index);
            if (region.container() || !region.playbackTarget()) continue;
            if (decidedRegions.contains(region.id())) continue;
            treatment(region, element, result, responseFingerprint)
                    .ifPresent(treatments::add);
        }
        if (regions.isEmpty()) warnings.add(
                "La pagina no contiene unidades semanticas utilizables.");
        long cpuNow = currentThreadCpuNanos();
        PdfPagePreparationMetrics metrics = new PdfPagePreparationMetrics(
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        Math.max(0L, System.nanoTime() - startedNanos)),
                startedCpu < 0L || cpuNow < 0L ? 0L
                        : java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        Math.max(0L, cpuNow - startedCpu)),
                Math.max(startedHeap, usedHeapBytes()), false, false);
        PdfDocumentLanguageProfile previousLanguage = previous == null
                ? PdfDocumentLanguageProfile.undetermined()
                : previous.analysisProfile().language();
        PdfDocumentLanguageProfile language = new PdfDocumentLanguageProfile(
                analysis.language(), 0.0,
                previousLanguage.overrideLanguage());
        PdfPageAnalysisProfile profile = new PdfPageAnalysisProfile(
                analysis.pageRole(),
                previous == null ? null : previous.analysisProfile().roleOverride(),
                language, PdfPreparationProfile.ENHANCED,
                previous == null ? 0
                        : previous.analysisProfile().normalizedRotationDegrees(),
                List.of(SEMANTIC_SIGNATURE,
                        result.diagnostics().getOrDefault("engineId", "content-analysis"),
                        result.diagnostics().getOrDefault("model", "qwen-local")));
        PdfPagePreparationStatus status = warnings.isEmpty()
                ? PdfPagePreparationStatus.READY
                : PdfPagePreparationStatus.READY_WITH_WARNINGS;
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                rendered.pageNumber(), rendered.pageWidthPoints(),
                rendered.pageHeightPoints(), status,
                previous == null ? 1L : previous.revision() + 1L,
                regions, treatments, metrics, profile,
                warnings.stream().distinct().toList(), "");
    }

    private void recordAttempt(PreparedPdfWorkspaceRef workspace,
                               int pageNumber,
                               String attemptId,
                               Instant started,
                               PdfOperationAttemptState state,
                               ContentAnalysisResult result,
                               PdfPageRenderResult rendered,
                               String diagnostic) {
        recordAttempt(workspace, pageNumber, attemptId, started, state, result,
                rendered, diagnostic, Map.of());
    }

    private void recordAttempt(PreparedPdfWorkspaceRef workspace,
                               int pageNumber,
                               String attemptId,
                               Instant started,
                               PdfOperationAttemptState state,
                               ContentAnalysisResult result,
                               PdfPageRenderResult rendered,
                               String diagnostic,
                               Map<String, String> failureDiagnostics) {
        try {
            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            if (failureDiagnostics != null) values.putAll(failureDiagnostics);
            if (result != null) values.putAll(result.diagnostics());
            PdfOperationMetrics metrics = new PdfOperationMetrics(
                    millis(values, "durationMs",
                            Math.max(0L, java.time.Duration.between(
                                    started, Instant.now()).toMillis())),
                    millis(values, "timeToFirstTokenMs", 0L),
                    millis(values, "promptTokens", 0L),
                    millis(values, "outputTokens", 0L),
                    result == null ? 0L : result.structuredJson().length(),
                    millis(values, "residentSetBytes", 0L),
                    millis(values, "effectiveVramBytes", 0L), 0L,
                    rendered == null ? 0 : rendered.image().getWidth(),
                    rendered == null ? 0 : rendered.image().getHeight(),
                    values.getOrDefault("engineId", "content-analysis"),
                    values.getOrDefault("model", ""),
                    values.getOrDefault("doneReason", ""));
            attempts.save(workspace.projectRoot(), new PdfOperationAttempt(
                    PdfOperationAttempt.CURRENT_SCHEMA_VERSION,
                    attemptId,
                    "PAGE_SEMANTIC_READING-" + workspace.sourceSha256()
                            + "-" + pageNumber,
                    pageNumber, "PAGE_SEMANTIC_READING", List.of(),
                    workspace.sourceSha256(),
                    sha256(String.join("|", TRANSPORT_VERSION,
                            PROMPT_VERSION, VERIFIER_STRATEGY_VERSION,
                            "ctx=8192", "primaryOutput=1800",
                            "verifierOutput=1800", "batch=512")),
                    state, started, Instant.now(), metrics, "", diagnostic));
        } catch (IOException | RuntimeException persistenceFailure) {
            LOGGER.warn("pdf.semantic.attempt-not-persisted page={} state={} reason={}",
                    pageNumber, state, persistenceFailure.toString());
        }
    }

    private static long millis(Map<String, String> values, String key,
                               long fallback) {
        try {
            return Long.parseLong(values.getOrDefault(key,
                    Long.toString(fallback)));
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    static void cleanupTemporaryDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        List<Path> managedPaths;
        try (var paths = Files.walk(directory)) {
            managedPaths = paths.sorted(Comparator.reverseOrder()).toList();
        } catch (IOException cleanupFailure) {
            LOGGER.warn("pdf.semantic.temp-cleanup-failed name={} reason={}",
                    directory.getFileName(), cleanupFailure.getClass().getSimpleName());
            return;
        }
        for (Path path : managedPaths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException cleanupFailure) {
                LOGGER.warn("pdf.semantic.temp-cleanup-failed name={} reason={}",
                        path.getFileName(), cleanupFailure.getClass().getSimpleName());
            }
        }
    }

    private static String diagnostic(Throwable failure,
                                     ContentAnalysisResult result) {
        String raw = failure instanceof PdfSemanticCoverageException coverageFailure
                ? coverageFailure.rawOutput()
                : result == null ? "" : result.structuredJson();
        String engineDiagnostics = failure instanceof EngineExecutionException engine
                && !engine.diagnostics().isEmpty()
                ? "\n--- ENGINE DIAGNOSTICS ---\n" + engine.diagnostics()
                : failure instanceof PdfSemanticProtocolException protocol
                && !protocol.diagnostics().isEmpty()
                ? "\n--- PROTOCOL DIAGNOSTICS ---\n" + protocol.diagnostics()
                : "";
        String coverageDiagnostics = failure instanceof PdfSemanticCoverageException coverage
                ? "\n--- COVERAGE DIAGNOSTICS ---\n"
                + "status=" + coverage.result().status()
                + "\nreliableNativeText=" + coverage.result().reliableNativeText()
                + "\ncoveredTokenRatio=" + number(coverage.result().coveredTokenRatio())
                + "\nreasons=" + coverage.result().reasons()
                + "\nmissingEvidence=" + coverage.result().missingEvidence()
                : "";
        String semanticTrace = failure instanceof PdfSemanticCoverageException coverage
                && !coverage.diagnostics().isEmpty()
                ? "\n--- SEMANTIC TRACE ---\n" + formattedTrace(coverage.diagnostics())
                : "";
        return failure.getClass().getSimpleName() + ": "
                + Objects.toString(failure.getMessage(), "")
                + engineDiagnostics
                + coverageDiagnostics
                + semanticTrace
                + (raw.isBlank() ? "" : "\n--- RAW OUTPUT ---\n" + raw);
    }

    private static String successfulDiagnostic(ContentAnalysisResult result) {
        LinkedHashMap<String, String> trace = new LinkedHashMap<>();
        result.diagnostics().forEach((key, value) -> {
            if (key.startsWith("semanticTrace.")) {
                trace.put(key.substring("semanticTrace.".length()), value);
            }
        });
        return "--- SEMANTIC TRACE ---\n" + formattedTrace(trace);
    }

    private static String formattedTrace(Map<String, String> trace) {
        return trace.entrySet().stream()
                .filter(entry -> !entry.getKey().toLowerCase(Locale.ROOT)
                        .contains("base64"))
                .map(entry -> "[" + entry.getKey() + "]\n"
                        + Objects.toString(entry.getValue(), ""))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private java.util.Optional<PdfDerivedTreatment> treatment(
            PdfRegion region,
            PdfSemanticPageAnalysis.Element element,
            ContentAnalysisResult result,
            String responseFingerprint) {
        String narration = element.narrationText();
        if (narration.isBlank()) return java.util.Optional.empty();
        boolean secondary = region.automaticType() == PdfRegionType.TABLE
                || region.automaticType() == PdfRegionType.MATH
                || region.automaticType() == PdfRegionType.IMAGE;
        if (!secondary && normalized(narration).equals(
                normalized(element.sourceText()))) {
            return java.util.Optional.empty();
        }
        PdfDerivedTreatmentKind kind = switch (region.automaticType()) {
            case TABLE -> PdfDerivedTreatmentKind.TABLE_NARRATION;
            case MATH -> PdfDerivedTreatmentKind.MATHEMATICAL_READING;
            case IMAGE -> PdfDerivedTreatmentKind.IMAGE_DESCRIPTION;
            default -> PdfDerivedTreatmentKind.LIGHTWEIGHT_LANGUAGE_MODEL;
        };
        String sourceFingerprint = sha256(String.join("|",
                region.id(), Long.toString(region.revision()),
                responseFingerprint, TRANSPORT_VERSION, PROMPT_VERSION));
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("automaticAdmission",
                PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION);
        metadata.put("groundingStatus", "OK");
        metadata.put("sourceFingerprintValidated", "true");
        metadata.put("semanticStrategy", "BRIEF");
        metadata.put("semanticResponseFingerprint", responseFingerprint);
        metadata.put("semanticTransportVersion", TRANSPORT_VERSION);
        metadata.put("promptVersion", PROMPT_VERSION);
        metadata.put("focusBounds", String.join(",",
                number(region.xMin()), number(region.yMin()),
                number(region.xMax()), number(region.yMax())));
        metadata.put("ttsSafetyValidated", Boolean.toString(
                narrationSafety.safeText(kind, narration, "BRIEF")));
        return java.util.Optional.of(new PdfDerivedTreatment(
                "SEM-" + sha256(region.id() + "|" + responseFingerprint)
                        .substring(0, 24).toUpperCase(Locale.ROOT),
                kind, List.of(region.id()), narration,
                result.diagnostics().getOrDefault("model", "qwen-local"),
                SEMANTIC_SIGNATURE, 0.0, Instant.now(),
                PdfDerivedTreatmentState.DRAFT, region.revision(),
                sourceFingerprint, INSTRUCTION, metadata));
    }

    private static List<PdfDerivedTreatment> preserveUserTreatments(
            PreparedPdfPage previous, List<PdfRegion> regions) {
        if (previous == null || previous.derivedTreatments().isEmpty()) {
            return List.of();
        }
        Map<String, PdfRegion> byId = regions.stream().collect(
                java.util.stream.Collectors.toMap(PdfRegion::id, value -> value));
        ArrayList<PdfDerivedTreatment> result = new ArrayList<>();
        for (PdfDerivedTreatment treatment : previous.derivedTreatments()) {
            boolean userDecision = treatment.state() != PdfDerivedTreatmentState.DRAFT
                    || "manual-user".equals(treatment.modelId())
                    || "manual".equals(treatment.metadata().get("source"));
            if (!userDecision || treatment.sourceRegionIds().stream()
                    .anyMatch(id -> !byId.containsKey(id))) continue;
            long revision = treatment.sourceRegionIds().stream()
                    .map(byId::get).mapToLong(PdfRegion::revision).max().orElse(1L);
            result.add(new PdfDerivedTreatment(
                    treatment.id(), treatment.kind(), treatment.sourceRegionIds(),
                    treatment.derivedText(), treatment.modelId(), treatment.modelVersion(),
                    treatment.confidence(), treatment.createdAt(), treatment.state(),
                    revision, treatment.sourceFingerprint(), treatment.prompt(),
                    treatment.metadata()));
        }
        return List.copyOf(result);
    }

    private static PdfNarratability effectiveNarratability(
            PdfSemanticPageAnalysis.Element element) {
        if ("CONTAINER".equals(element.attributes().get("regionRole"))
                || !Boolean.parseBoolean(element.attributes()
                .getOrDefault("playbackTarget", "true"))) {
            return PdfNarratability.NON_NARRATABLE;
        }
        if (element.type() == PdfRegionType.HEADER
                || element.type() == PdfRegionType.FOOTER
                || element.type() == PdfRegionType.PAGE_NUMBER) {
            return PdfNarratability.NON_NARRATABLE;
        }
        return element.narratability();
    }

    static String mathSourceConvention(PdfRegionType type, String sourceText) {
        String text = sourceText == null ? "" : sourceText;
        if (type == PdfRegionType.MATH) return "STRUCTURED_MATH_V1";
        var inline = java.util.regex.Pattern.compile(
                "(?<!\\$)\\$([^$\\r\\n]+)\\$(?!\\$)").matcher(text);
        while (inline.find()) {
            String expression = inline.group(1);
            boolean technical = expression.matches(
                    "(?s).*(?:\\\\(?:frac|sqrt|sin|cos|tan|lim|pi|to)|"
                            + "[=<>/^]|(?:sin|cos|tan|lim)\\s+.*).*" );
            boolean monetary = expression.matches(
                    "(?i)\\s*(?:US\\$|USD\\s*)?\\d+(?:[.,]\\d{1,2})?\\s*");
            if (technical && !monetary) return "LATEX_TECHNICAL_V1";
        }
        return "";
    }

    private static String stableRegionId(
            String sourceSha, int page,
            PdfSemanticPageAnalysis.Element element,
            PdfSemanticPageAnalysis.NormalizedBox box) {
        String semanticEvidence = String.join("|",
                sourceSha, Integer.toString(page), normalized(element.sourceText()),
                number(box.xMin()), number(box.yMin()),
                number(box.xMax()), number(box.yMax()));
        return "PDF-R%06d-%s".formatted(page,
                sha256(semanticEvidence).substring(0, 20)
                        .toUpperCase(Locale.ROOT));
    }

    private static double points(double normalized, double dimension) {
        return (normalized / 1000.0) * dimension;
    }

    private static ComputeJobPriority computePriority(
            PdfPreparationPriority priority) {
        return priority == PdfPreparationPriority.URGENT
                ? ComputeJobPriority.INTERACTIVE_ANALYSIS
                : ComputeJobPriority.BACKGROUND;
    }

    private static String normalized(String value) {
        return java.text.Normalizer.normalize(
                        value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").strip();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value)
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static long currentThreadCpuNanos() {
        var bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported()
                ? bean.getCurrentThreadCpuTime() : -1L;
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
    }
}
