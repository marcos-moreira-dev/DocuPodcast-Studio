package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentLanguageProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttemptState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationMetrics;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPreparationProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNativeTextProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Canonical pipeline for preparing and atomically publishing one PDF page. */
public final class PreparePdfPageUseCase {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
            PreparePdfPageUseCase.class);
    private final BuildPdfOcrTextLayerUseCase ocrTextLayer;
    private final PdfTextLayerBlockMapper blockMapper;
    private final Supplier<OperationalSettings.OcrSettings> ocrSettingsSupplier;
    private final PreparedPdfDocumentRepository repository;
    private final PdfNativePageExtractor nativeExtractor;
    private PdfNativePageExtractor alternateNativeExtractor;

    public PreparePdfPageUseCase withAlternateNativeExtractor(PdfNativePageExtractor extractor) {
        alternateNativeExtractor = extractor;
        return this;
    }

    /** OCR uses the existing shared CPU admission; direct extraction needs no model lease. */
    public boolean requiresOcrAdmission(PreparePdfPageRequest request) {
        PdfReadingStrategy strategy = request.readingStrategy();
        if (strategy == null) strategy = loadManifest(request.workspace().projectRoot())
                .map(PdfDocumentManifest::readingStrategy).orElse(PdfReadingStrategy.SEMANTIC);
        return request.forceOcr() || strategy == PdfReadingStrategy.NATIVE_WITH_OCR;
    }
    private final AnalyzePdfPageSemanticallyUseCase semanticPageReader;
    private final PdfNativeTextQualityAssessor nativeQuality = new PdfNativeTextQualityAssessor();
    private final PdfAdaptiveOcrPolicy adaptiveOcr = new PdfAdaptiveOcrPolicy();
    private final PdfLocalLanguageDetector languageDetector = new PdfLocalLanguageDetector();
    private final PdfPageRoleClassifier pageRoleClassifier = new PdfPageRoleClassifier();

    public PreparePdfPageUseCase(BuildPdfOcrTextLayerUseCase ocrTextLayer,
                                 PdfTextLayerBlockMapper blockMapper,
                                 Supplier<OperationalSettings.OcrSettings> ocrSettingsSupplier,
                                 PreparedPdfDocumentRepository repository) {
        this(ocrTextLayer, blockMapper, ocrSettingsSupplier, repository, null, null);
    }

    public PreparePdfPageUseCase(BuildPdfOcrTextLayerUseCase ocrTextLayer,
                                 PdfTextLayerBlockMapper blockMapper,
                                 Supplier<OperationalSettings.OcrSettings> ocrSettingsSupplier,
                                 PreparedPdfDocumentRepository repository,
                                 PdfNativePageExtractor nativeExtractor) {
        this(ocrTextLayer, blockMapper, ocrSettingsSupplier, repository,
                nativeExtractor, null);
    }

    public PreparePdfPageUseCase(BuildPdfOcrTextLayerUseCase ocrTextLayer,
                                 PdfTextLayerBlockMapper blockMapper,
                                 Supplier<OperationalSettings.OcrSettings> ocrSettingsSupplier,
                                 PreparedPdfDocumentRepository repository,
                                 PdfNativePageExtractor nativeExtractor,
                                 AnalyzePdfPageSemanticallyUseCase semanticPageReader) {
        this.ocrTextLayer = Objects.requireNonNull(ocrTextLayer, "ocrTextLayer");
        this.blockMapper = blockMapper == null ? new PdfTextLayerBlockMapper() : blockMapper;
        this.ocrSettingsSupplier = ocrSettingsSupplier == null
                ? OperationalSettings.OcrSettings::defaults : ocrSettingsSupplier;
        this.repository = repository;
        this.nativeExtractor = nativeExtractor;
        this.semanticPageReader = semanticPageReader;
    }

    public PreparePdfPageResult execute(PreparePdfPageRequest request) {
        long startedNanos = System.nanoTime();
        long startedCpuNanos = currentThreadCpuNanos();
        long startedHeapBytes = usedHeapBytes();
        Objects.requireNonNull(request, "request");
        PreparedPdfWorkspaceRef workspace = request.workspace();
        int page = request.pageNumber();
        PdfPreparationCancellationToken cancellation = request.cancellationToken();
        ArrayList<DocumentImportIssue> issues = new ArrayList<>();
        Path projectRoot = workspace.projectRoot();
        PreparedPdfPage previous = null;
        try {
            cancellation.throwIfCancelled();
            PdfDocumentManifest manifest = loadManifest(projectRoot)
                    .orElseThrow(() -> new IOException("No existe document/manifest.json para el PDF."));
            if (!manifest.sourceSha256().equalsIgnoreCase(workspace.sourceSha256())) {
                throw new IOException("El hash del manifest no corresponde al PDF de la sesión.");
            }
            if (page > manifest.pageCount()) {
                return PreparePdfPageResult.technicalFailure(page, null, List.of(),
                        PdfOperationAttemptState.FAILED, "PAGE_OUT_OF_RANGE",
                        "La página solicitada no existe en el PDF.", null);
            }
            OperationalSettings.OcrSettings settings = currentOcrSettings();
            previous = loadPage(projectRoot, page).orElse(null);
            PdfReadingStrategy strategy = request.readingStrategy() == null ? manifest.readingStrategy() : request.readingStrategy();
            PdfNativeTextProvider provider = request.nativeTextProvider() == null ? manifest.nativeTextProvider() : request.nativeTextProvider();
            String preparationKey = "pdf-reading-v1:" + strategy.name() + ":" + provider.name();
            if (!request.forceOcr() && strategy != PdfReadingStrategy.SEMANTIC && previous != null
                    && previous.analysisProfile().analysisEngineIds().contains(preparationKey)
                    && previous.lastAttemptError().isBlank()) {
                request.progress().report("CACHE_HIT", 1.0, "Página " + page + " · ya preparada");
                return new PreparePdfPageResult(page, previous, true, false, issues);
            }
            if (strategy == PdfReadingStrategy.SEMANTIC && semanticPageReader != null && !request.forceOcr()) {
                if (semanticPageReader.current(previous)) {
                    LOGGER.info("[PDF][P{}] semantic cache HIT action=SKIPPED reason=CANONICAL_CACHE_HIT origin={} scopeId={} regions={}",
                            page, request.origin(), request.scopeId(), previous.regions().size());
                    request.progress().report("CACHE_HIT", 1.0,
                            "Página " + page + " · ya preparada");
                    issues.add(DocumentImportIssue.info("pdf-semantic-page-reused",
                            "Pagina PDF reutilizada desde la lectura semantica persistida."));
                    return new PreparePdfPageResult(
                            page, previous, true, false, issues);
                }
                java.util.Optional<PdfOperationAttemptState> previousState =
                        semanticPageReader.lastAttemptState(projectRoot, page);
                if (!request.retryAllowed() && previousState
                        .filter(PreparePdfPageUseCase::terminalNonAccepted)
                        .isPresent()) {
                    PdfOperationAttemptState state = previousState.orElseThrow();
                    LOGGER.info("[PDF][P{}] semantic action=SKIPPED reason=RETRY_NOT_AUTHORIZED origin={} scopeId={} previousState={} retryAllowed=false",
                            page, request.origin(), request.scopeId(), state);
                    request.progress().report("RETRY_SKIPPED", 1.0,
                            "Página " + page + " · requiere reintento explícito");
                    String reason = state == PdfOperationAttemptState.INSUFFICIENT_EVIDENCE
                            ? "El análisis anterior no pudo interpretarse con suficiente fiabilidad."
                            : "El análisis anterior no pudo completarse. Usa Procesar o Reintentar para intentarlo expresamente.";
                    return state == PdfOperationAttemptState.INSUFFICIENT_EVIDENCE
                            ? PreparePdfPageResult.rejected(page, previous, issues, reason, null)
                            : PreparePdfPageResult.technicalFailure(page, previous, issues,
                                    state, "RETRY_NOT_AUTHORIZED", reason, null);
                }
                String retryReason = semanticPageReader.manualRetryReason(projectRoot, page);
                LOGGER.info("[PDF][P{}] semantic request origin={} scopeId={} retryAllowed={} previousState={} decision=START reason={}",
                        page, request.origin(), request.scopeId(), request.retryAllowed(),
                        previousState.map(Enum::name).orElse("NOT_ATTEMPTED"), retryReason);
                request.progress().report("SEMANTIC_START", 0.01,
                        "CANONICAL_CACHE_MISS".equals(retryReason)
                                ? "Página " + page + " · iniciando análisis"
                                : "Página " + page + " · reintentando análisis");
                PreparedPdfPage prepared = semanticPageReader.analyze(
                        workspace, page, previous, cancellation, request.priority(),
                        request.progress(), request.origin(), request.scopeId(),
                        request.retryAllowed());
                cancellation.throwIfCancelled();
                request.progress().report("SAVING", 0.98,
                        "Guardando pagina " + page + ".");
                savePage(projectRoot, prepared);
                request.progress().report("SAVED", 1.0,
                        "Pagina " + page + " guardada.");
                issues.add(DocumentImportIssue.info("pdf-semantic-page-applied",
                        "Pagina PDF leida visualmente y publicada en regiones semanticas."));
                return new PreparePdfPageResult(
                        page, prepared, true, false, issues);
            }
            request.progress().report("NATIVE_TEXT", 0.05, "Página " + page + " · extrayendo texto del PDF");
            PdfNativePageExtractor extractor = provider == PdfNativeTextProvider.POPPLER
                    ? alternateNativeExtractor : nativeExtractor;
            PdfTextLayer nativeLayer = extractor == null
                    ? new PdfTextLayer(page, PdfTextLayerOrigin.UNAVAILABLE, List.of(), List.of())
                    : extractor.extract(workspace.sourcePath(), page);
            PdfNativeTextQualityReport nativeReport = nativeQuality.assess(nativeLayer);
            if (!request.forceOcr() && strategy == PdfReadingStrategy.NATIVE_TEXT && !nativeLayer.available()) {
                String message = "Página " + page + ": no se pudo obtener texto directamente. "
                        + "Puedes elegir Lectura con reconocimiento para una página escaneada.";
                issues.add(DocumentImportIssue.warning("pdf-native-text-unavailable", message));
                nativeLayer.warnings().forEach(warning -> issues.add(DocumentImportIssue.warning(
                        "pdf-native-extraction-detail", warning)));
                request.progress().report("NATIVE_UNAVAILABLE", 1.0, message);
                return PreparePdfPageResult.technicalFailure(page, previous, issues,
                        PdfOperationAttemptState.INSUFFICIENT_EVIDENCE, "NATIVE_TEXT_UNAVAILABLE", message, null);
            }
            if (!request.forceOcr()
                    && (strategy == PdfReadingStrategy.NATIVE_TEXT && nativeLayer.available()
                    || nativeReport.quality() == PdfNativeTextQuality.RELIABLE
                    || nativeReport.quality() == PdfNativeTextQuality.SUSPECT
                    && nativeReport.reliableRegionRatio() >= 0.88)) {
                cancellation.throwIfCancelled();
                PreparedPdfPage prepared = nativePage(nativeLayer, manifest.sourceSha256(), previous,
                        nativeReport.reasons(), provider);
                prepared = applyNativeRegionQuality(prepared, nativeReport);
                prepared = withAnalysisProfile(prepared, nativeLayer, PdfPreparationProfile.STANDARD);
                prepared = withMetrics(prepared, startedNanos, startedCpuNanos, startedHeapBytes,
                        true, false);
                cancellation.throwIfCancelled();
                prepared = preserveReviewedContent(prepared, previous);
                prepared = saveAndAnalyze(projectRoot, withPreparationKey(prepared, preparationKey), previous == null);
                issues.add(DocumentImportIssue.info("pdf-native-text-applied",
                        "Página PDF preparada desde texto nativo fiable; no se ejecutó OCR."));
                return new PreparePdfPageResult(page, prepared, true, false, issues);
            }
            int psm = adaptiveOcr.pageSegmentationMode(nativeLayer);
            PdfOcrPageResult result = ocrTextLayer.recognize(new PdfOcrRequest(
                    workspace.sourcePath(), page, settings.dpi(), PdfOcrRequest.DEFAULT_MAX_PIXEL_COUNT,
                    settings.languages(), request.forceOcr(),
                    cacheDirectory(request.cacheDirectory(), settings.cacheEnabled()), psm, 0));
            cancellation.throwIfCancelled();
            PreparedPdfPage prepared = blockMapper.preparedPage(result, manifest.sourceSha256(), previous);
            if (nativeReport.quality() == PdfNativeTextQuality.SUSPECT && nativeLayer.available()) {
                PreparedPdfPage nativeCandidate = nativePage(nativeLayer, manifest.sourceSha256(), previous,
                        nativeReport.reasons(), provider);
                prepared = mergeHybrid(nativeCandidate, prepared);
                issues.add(DocumentImportIssue.info("pdf-hybrid-text-applied",
                        "Se reconciliaron evidencias nativas y OCR para la página " + page + "."));
            }
            if (prepared.regions().isEmpty()) {
                issues.add(DocumentImportIssue.warning("pdf-ocr-empty",
                        "OCR p. " + page + ": no se detectó texto; la página quedó preparada sin regiones."));
            }
            prepared = withAnalysisProfile(prepared,
                    result.textLayer().available() ? result.textLayer() : nativeLayer,
                    PdfPreparationProfile.STANDARD);
            prepared = withMetrics(prepared, startedNanos, startedCpuNanos, startedHeapBytes,
                    nativeLayer.available(), true);
            result.warnings().forEach(warning -> issues.add(DocumentImportIssue.warning(
                    "pdf-ocr-page-warning", "OCR p. " + page + ": " + warning)));
            cancellation.throwIfCancelled();
            prepared = preserveReviewedContent(prepared, previous);
            prepared = saveAndAnalyze(projectRoot, withPreparationKey(prepared,
                    request.forceOcr() ? "pdf-reading-v1:FORCED_OCR" : preparationKey), previous == null);
            issues.add(DocumentImportIssue.info("pdf-ocr-applied",
                    "Página PDF preparada en regiones V2; se conservaron regiones no narrables o dudosas."));
            return new PreparePdfPageResult(page, prepared, true, false, issues);
        } catch (PdfPreparationCancelledException ex) {
            return PreparePdfPageResult.cancelled(page, previous);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return PreparePdfPageResult.cancelled(page, previous);
        } catch (PdfOcrException ex) {
            recordFailedAttempt(projectRoot, page, ex.getMessage());
            if (ex.code() == PdfOcrErrorCode.TESSERACT_NOT_FOUND) {
                issues.add(DocumentImportIssue.warning("pdf-ocr-runtime-missing",
                        "OCR p. " + page + ": OCR no configurado. Importa Tesseract portable o configúralo."));
            } else {
                issues.add(DocumentImportIssue.warning("pdf-ocr-unavailable",
                        "OCR p. " + page + ": [" + ex.code() + "] " + ex.getMessage()));
            }
            return PreparePdfPageResult.technicalFailure(page, previous, issues,
                    PdfOperationAttemptState.FAILED, "OCR_" + ex.code(),
                    issues.getFirst().message(), ex);
        } catch (IOException ex) {
            recordFailedAttempt(projectRoot, page, ex.getMessage());
            issues.add(DocumentImportIssue.warning(semanticFailureCode(ex),
                    semanticFailureMessage(ex, page)));
            if (ex instanceof PdfSemanticCoverageException) {
                return PreparePdfPageResult.rejected(page, previous, issues,
                        issues.getFirst().message(), ex);
            }
            return PreparePdfPageResult.technicalFailure(page, previous, issues,
                    semanticFailureState(ex), semanticFailureCategory(ex),
                    issues.getFirst().message(), ex);
        }
    }

    private static boolean terminalNonAccepted(PdfOperationAttemptState state) {
        return state == PdfOperationAttemptState.FAILED
                || state == PdfOperationAttemptState.TRUNCATED
                || state == PdfOperationAttemptState.INSUFFICIENT_EVIDENCE
                || state == PdfOperationAttemptState.CANCELLED
                || state == PdfOperationAttemptState.INTERRUPTED;
    }

    private PreparedPdfPage nativePage(PdfTextLayer layer,
                                       String sourceSha,
                                       PreparedPdfPage previous,
                                       List<String> warnings, PdfNativeTextProvider provider) {
        PdfPageRegion region = layer.lines().getFirst().region();
        return blockMapper.preparedPage(layer, region.pageWidthPoints(), region.pageHeightPoints(),
                sourceSha, previous, PdfRegionOrigin.NATIVE_TEXT,
                provider == PdfNativeTextProvider.POPPLER ? "poppler-bbox" : "pdfbox-native-v1", warnings);
    }

    private static PreparedPdfPage withPreparationKey(PreparedPdfPage page, String key) {
        PdfPageAnalysisProfile profile = page.analysisProfile();
        ArrayList<String> engines = new ArrayList<>(profile.analysisEngineIds());
        engines.removeIf(value -> value.startsWith("pdf-reading-v1:"));
        engines.add(key);
        return new PreparedPdfPage(page.schemaVersion(), page.pageNumber(), page.widthPoints(),
                page.heightPoints(), page.status(), page.revision(), page.regions(), page.derivedTreatments(),
                page.preparationMetrics(), new PdfPageAnalysisProfile(profile.automaticRole(),
                profile.roleOverride(), profile.language(), profile.preparationProfile(),
                profile.normalizedRotationDegrees(), engines), page.warnings(), page.lastAttemptError());
    }

    private static PreparedPdfPage preserveReviewedContent(PreparedPdfPage page, PreparedPdfPage previous)
            throws IOException {
        if (previous == null) return page;
        var ids = page.regions().stream().map(PdfRegion::id).collect(java.util.stream.Collectors.toSet());
        boolean unmatchedReview = previous.regions().stream()
                .anyMatch(region -> !region.override().emptyOverride() && !ids.contains(region.id()));
        boolean unmatchedTreatment = previous.derivedTreatments().stream()
                .flatMap(treatment -> treatment.sourceRegionIds().stream()).anyMatch(id -> !ids.contains(id));
        if (unmatchedReview || unmatchedTreatment) {
            throw new IOException("La nueva lectura no coincide con contenido revisado de esta página. "
                    + "Se conserva la preparación anterior; revisa las regiones antes de cambiar su lectura.");
        }
        return new PreparedPdfPage(page.schemaVersion(), page.pageNumber(), page.widthPoints(), page.heightPoints(),
                page.status(), page.revision(), page.regions(), previous.derivedTreatments(),
                page.preparationMetrics(), page.analysisProfile(), page.warnings(), page.lastAttemptError());
    }

    private static PreparedPdfPage mergeHybrid(PreparedPdfPage nativePage, PreparedPdfPage ocrPage) {
        ArrayList<PdfRegion> merged = new ArrayList<>();
        java.util.HashSet<String> matchedNative = new java.util.HashSet<>();
        for (PdfRegion ocr : ocrPage.regions()) {
            PdfRegion nativeRegion = nativePage.regions().stream()
                    .filter(candidate -> !matchedNative.contains(candidate.id()))
                    .max(java.util.Comparator.comparingDouble(candidate -> overlap(candidate, ocr)))
                    .filter(candidate -> overlap(candidate, ocr) >= 0.18)
                    .orElse(null);
            if (nativeRegion == null) {
                merged.add(ocr);
                continue;
            }
            matchedNative.add(nativeRegion.id());
            boolean compatible = compatibleText(nativeRegion.text(), ocr.text());
            ArrayList<String> reasons = new ArrayList<>(ocr.reasons());
            if (!compatible) reasons.add("native-ocr-disagreement");
            PdfRegionEvidence selected = new PdfRegionEvidence(
                    PdfRegionOrigin.HYBRID,
                    (nativeRegion.evidence().confidence() + ocr.evidence().confidence()) / 2.0,
                    nativeRegion.evidence().extractorVersion() + "+tesseract", "hybrid-v1", "paragraph-v2", "narratability-v2");
            ArrayList<PdfRegionEvidence> candidates = new ArrayList<>();
            candidates.add(nativeRegion.evidence());
            candidates.addAll(nativeRegion.evidenceCandidates());
            candidates.add(ocr.evidence());
            candidates.addAll(ocr.evidenceCandidates());
            merged.add(new PdfRegion(
                    ocr.id(), ocr.pageNumber(), ocr.xMin(), ocr.yMin(), ocr.xMax(), ocr.yMax(),
                    ocr.columnIndex(), ocr.readingOrder(),
                    compatible ? nativeRegion.text() : ocr.text(),
                    ocr.automaticType(),
                    compatible ? ocr.automaticNarratability() : PdfNarratability.UNCERTAIN,
                    reasons, selected, candidates, ocr.override(), ocr.attributes(), ocr.revision()));
        }
        for (PdfRegion nativeRegion : nativePage.regions()) {
            if (matchedNative.contains(nativeRegion.id())) continue;
            ArrayList<String> reasons = new ArrayList<>(nativeRegion.reasons());
            reasons.add("native-region-not-confirmed-by-ocr");
            merged.add(new PdfRegion(
                    nativeRegion.id(), nativeRegion.pageNumber(),
                    nativeRegion.xMin(), nativeRegion.yMin(), nativeRegion.xMax(), nativeRegion.yMax(),
                    nativeRegion.columnIndex(), nativeRegion.readingOrder(), nativeRegion.text(),
                    nativeRegion.automaticType(), PdfNarratability.UNCERTAIN, reasons,
                    nativeRegion.evidence(), nativeRegion.evidenceCandidates(), nativeRegion.override(),
                    nativeRegion.attributes(), nativeRegion.revision()));
        }
        ArrayList<String> warnings = new ArrayList<>(ocrPage.warnings());
        warnings.add("Página híbrida: se conservaron evidencias nativas y OCR.");
        return new PreparedPdfPage(ocrPage.schemaVersion(), ocrPage.pageNumber(),
                ocrPage.widthPoints(), ocrPage.heightPoints(),
                PdfPagePreparationStatus.READY_WITH_WARNINGS,
                Math.max(nativePage.revision(), ocrPage.revision()),
                merged, ocrPage.derivedTreatments(), ocrPage.preparationMetrics(),
                ocrPage.analysisProfile(), warnings, "");
    }

    private static boolean compatibleText(String first, String second) {
        String left = normalized(first);
        String right = normalized(second);
        if (left.isBlank() || right.isBlank()) return false;
        return left.equals(right) || (Math.min(left.length(), right.length()) >= 12
                && (left.contains(right) || right.contains(left)));
    }

    private static String normalized(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value,
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ").strip();
    }

    private static double overlap(PdfRegion a, PdfRegion b) {
        double width = Math.max(0.0, Math.min(a.xMax(), b.xMax()) - Math.max(a.xMin(), b.xMin()));
        double height = Math.max(0.0, Math.min(a.yMax(), b.yMax()) - Math.max(a.yMin(), b.yMin()));
        double intersection = width * height;
        double union = area(a) + area(b) - intersection;
        return union <= 0.0 ? 0.0 : intersection / union;
    }

    private static double area(PdfRegion region) {
        return (region.xMax() - region.xMin()) * (region.yMax() - region.yMin());
    }

    public String sourceSha256(PreparedPdfWorkspaceRef workspace) {
        return Objects.requireNonNull(workspace, "workspace").sourceSha256();
    }

    private OperationalSettings.OcrSettings currentOcrSettings() {
        try {
            OperationalSettings.OcrSettings settings = ocrSettingsSupplier.get();
            return settings == null ? OperationalSettings.OcrSettings.defaults() : settings;
        } catch (RuntimeException ex) {
            return OperationalSettings.OcrSettings.defaults();
        }
    }

    private Optional<PdfDocumentManifest> loadManifest(Path root) {
        if (repository == null || root == null) return Optional.empty();
        try {
            return repository.loadManifest(root);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private Optional<PreparedPdfPage> loadPage(Path root, int page) throws IOException {
        return repository == null || root == null ? Optional.empty() : repository.loadPage(root, page);
    }

    private void savePage(Path root, PreparedPdfPage page) throws IOException {
        if (repository != null && root != null) repository.savePage(root, page);
    }

    private PreparedPdfPage saveAndAnalyze(Path root, PreparedPdfPage page, boolean newlyPrepared)
            throws IOException {
        savePage(root, page);
        if (repository == null || root == null) return page;
        PdfDocumentManifest current = repository.loadManifest(root).orElse(null);
        if (current == null) return page;
        PdfLayoutAnalysisResult analysis = new PdfDocumentLayoutAnalyzer()
                .analyzeIncremental(current, page, newlyPrepared);
        for (PreparedPdfPage analyzed : analysis.pages()) {
            if (analysis.changedPages().contains(analyzed.pageNumber())) {
                repository.savePage(root, analyzed);
            }
        }
        repository.saveManifest(root, analysis.manifest());
        return repository.loadPage(root, page.pageNumber()).orElse(page);
    }

    private static PreparedPdfPage withMetrics(PreparedPdfPage page,
                                               long startedNanos,
                                               long startedCpuNanos,
                                               long startedHeapBytes,
                                               boolean nativeUsed,
                                               boolean ocrUsed) {
        long cpu = currentThreadCpuNanos();
        PdfPagePreparationMetrics metrics = new PdfPagePreparationMetrics(
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        Math.max(0L, System.nanoTime() - startedNanos)),
                startedCpuNanos < 0L || cpu < 0L ? 0L
                        : java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        Math.max(0L, cpu - startedCpuNanos)),
                Math.max(startedHeapBytes, usedHeapBytes()),
                nativeUsed,
                ocrUsed);
        return new PreparedPdfPage(page.schemaVersion(), page.pageNumber(),
                page.widthPoints(), page.heightPoints(), page.status(), page.revision(),
                page.regions(), page.derivedTreatments(), metrics, page.analysisProfile(), page.warnings(),
                page.lastAttemptError());
    }

    private PreparedPdfPage withAnalysisProfile(PreparedPdfPage page,
                                                PdfTextLayer textLayer,
                                                PdfPreparationProfile preparationProfile) {
        String text = page.regions().stream().map(PdfRegion::effectiveText)
                .reduce("", (a, b) -> (a + "\n" + b).strip());
        PdfDocumentLanguageProfile detected = languageDetector.detect(text);
        PdfPageAnalysisProfile previous = page.analysisProfile();
        PdfDocumentLanguageProfile language = new PdfDocumentLanguageProfile(
                detected.detectedLanguage(), detected.confidence(),
                previous.language().overrideLanguage());
        PdfPageRole role = pageRoleClassifier.classify(page.regions());
        PdfPageAnalysisProfile profile = new PdfPageAnalysisProfile(
                role, previous.roleOverride(), language, preparationProfile,
                previous.normalizedRotationDegrees(),
                List.of(textLayer != null && textLayer.origin() == PdfTextLayerOrigin.NATIVE_BBOX
                        ? "native-regional-quality" : "tesseract-adaptive-psm"));
        return new PreparedPdfPage(page.schemaVersion(), page.pageNumber(), page.widthPoints(),
                page.heightPoints(), page.status(), page.revision(), page.regions(),
                page.derivedTreatments(), page.preparationMetrics(), profile,
                page.warnings(), page.lastAttemptError());
    }

    private static PreparedPdfPage applyNativeRegionQuality(
            PreparedPdfPage page, PdfNativeTextQualityReport report) {
        if (report.regions().isEmpty() || report.regions().size() != page.regions().size()) return page;
        ArrayList<PdfRegion> regions = new ArrayList<>();
        for (int index = 0; index < page.regions().size(); index++) {
            PdfRegion region = page.regions().get(index);
            PdfNativeRegionQualityReport quality = report.regions().get(index);
            ArrayList<String> reasons = new ArrayList<>(region.reasons());
            reasons.addAll(quality.reasons());
            PdfNarratability narratability = quality.quality() == PdfNativeTextQuality.RELIABLE
                    ? region.automaticNarratability() : PdfNarratability.UNCERTAIN;
            PdfRegionEvidence evidence = new PdfRegionEvidence(region.evidence().origin(),
                    Math.min(region.evidence().confidence(), quality.score()),
                    region.evidence().extractorVersion(), region.evidence().parserVersion(),
                    region.evidence().groupingVersion(), region.evidence().classifierVersion());
            regions.add(new PdfRegion(region.id(), region.pageNumber(), region.xMin(), region.yMin(),
                    region.xMax(), region.yMax(), region.columnIndex(), region.readingOrder(),
                    region.text(), region.automaticType(), narratability, reasons, evidence,
                    region.evidenceCandidates(), region.override(), region.attributes(), region.revision()));
        }
        return new PreparedPdfPage(page.schemaVersion(), page.pageNumber(), page.widthPoints(),
                page.heightPoints(), page.status(), page.revision(), regions, page.derivedTreatments(),
                page.preparationMetrics(), page.analysisProfile(), page.warnings(), page.lastAttemptError());
    }

    private static long currentThreadCpuNanos() {
        java.lang.management.ThreadMXBean bean =
                java.lang.management.ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1L;
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
    }

    private void recordFailedAttempt(Path root, int pageNumber, String error) {
        if (repository == null || root == null) return;
        try {
            Optional<PreparedPdfPage> previous = repository.loadPage(root, pageNumber);
            if (previous.isEmpty()) return;
            PreparedPdfPage page = previous.get();
            repository.savePage(root, new PreparedPdfPage(
                    page.schemaVersion(), page.pageNumber(), page.widthPoints(), page.heightPoints(),
                    page.status(), page.revision(), page.regions(), page.derivedTreatments(),
                    page.preparationMetrics(), page.analysisProfile(), page.warnings(),
                    error == null || error.isBlank() ? "Falló el último intento de preparación." : error));
        } catch (IOException ignored) {
            // Preserve the last valid page if even the diagnostic cannot be published.
        }
    }

    private static String semanticFailureCode(IOException failure) {
        if (failure instanceof PdfSemanticCoverageException) {
            return "pdf-semantic-coverage-insufficient";
        }
        if (failure instanceof PdfSemanticProtocolException protocol) {
            return protocol.kind() == PdfSemanticProtocolException.Kind.INCOMPLETE
                    ? "pdf-semantic-output-truncated"
                    : "pdf-semantic-protocol-invalid";
        }
        if (failure instanceof com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException engine) {
            return switch (engine.code()) {
                case TRANSPORT_TRANSIENT -> "pdf-semantic-backend-transient";
                case REQUEST_TIMEOUT, READINESS_TIMEOUT, RUNTIME_UNRESPONSIVE ->
                        "pdf-semantic-timeout";
                case REQUEST_STALL -> "pdf-semantic-stall";
                case OOM -> "pdf-semantic-oom";
                case OUTPUT_TRUNCATED -> "pdf-semantic-output-truncated";
                case REQUEST_REJECTED -> "pdf-semantic-request-rejected";
                case PROTOCOL_INVALID, INVALID_OUTPUT -> "pdf-semantic-protocol-invalid";
                case CANCELLED -> "pdf-semantic-cancelled";
                default -> "pdf-semantic-analysis-failed";
            };
        }
        return "pdf-v2-persistence-failed";
    }

    private static PdfOperationAttemptState semanticFailureState(
            IOException failure) {
        if (failure instanceof PdfSemanticProtocolException protocol
                && protocol.kind() == PdfSemanticProtocolException.Kind.INCOMPLETE) {
            return PdfOperationAttemptState.TRUNCATED;
        }
        if (failure instanceof com.marcosmoreiradev.docupodcaststudio.media.api
                .EngineExecutionException engine
                && engine.code() == com.marcosmoreiradev.docupodcaststudio.media.api
                .EngineDiagnosticCode.OUTPUT_TRUNCATED) {
            return PdfOperationAttemptState.TRUNCATED;
        }
        return PdfOperationAttemptState.FAILED;
    }

    private static String semanticFailureCategory(IOException failure) {
        String code = semanticFailureCode(failure);
        return code.startsWith("pdf-")
                ? code.substring(4).replace('-', '_').toUpperCase(java.util.Locale.ROOT)
                : code.toUpperCase(java.util.Locale.ROOT);
    }

    static String semanticFailureMessage(IOException failure, int pageNumber) {
        String prefix = "PDF p. " + pageNumber + ": ";
        if (failure instanceof PdfSemanticCoverageException) {
            return prefix
                    + "esta página no pudo interpretarse con suficiente fiabilidad.";
        }
        if (failure instanceof PdfSemanticProtocolException protocol) {
            return prefix + (protocol.kind()
                    == PdfSemanticProtocolException.Kind.INCOMPLETE
                    ? "la lectura local quedó incompleta y no se publicó."
                    : "la respuesta del lector local no fue procesable y no se publicó.");
        }
        if (failure instanceof com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException) {
            return prefix
                    + "el motor local no pudo completar el análisis de esta página.";
        }
        return prefix + "no se pudo guardar la preparación de esta página.";
    }

    static Path projectRoot(Path sourcePath) {
        if (sourcePath == null) return null;
        Path parent = sourcePath.toAbsolutePath().normalize().getParent();
        if (parent == null || parent.getFileName() == null
                || !"source".equalsIgnoreCase(parent.getFileName().toString())) return null;
        return parent.getParent();
    }

    private static Path cacheDirectory(Path configured, boolean enabled) {
        if (!enabled) return null;
        return configured == null
                ? Path.of(System.getProperty("java.io.tmpdir"), "docupodcast-studio", "pdf-ocr")
                : configured;
    }

}
