package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScope;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScopeRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScopeResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfProjectSessionToken;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSelectionSnapshot;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentTechnicalElement;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfAnalysisVisualEvidence;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfDerivedTreatmentGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfRegionContext;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalMathPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalTableExplanationPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.TransversalVisualPdfTreatmentEngine;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticNarrationSafetyValidator;
import com.marcosmoreiradev.docupodcaststudio.application.document.SecondarySemanticComponentClassifier;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.DocumentAudioPreparationExtent;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotificationLevel;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.application.Platform;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.IntConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;

/** Explicit scope gate used before PDF reading and audio operations. */
public final class PdfNarratablePreparationCoordinator {
    private static final Pattern REGION_PAGE = Pattern.compile("^P(\\d+)-R");
    private final DocuPodcastShellViewModel viewModel;
    private final ExceptionAlertPresenter alertPresenter;
    private final Supplier<Window> ownerSupplier;
    private Path sessionSource;
    private PdfProjectSessionToken session = new PdfProjectSessionToken();
    private final AtomicLong localAnalysisGeneration = new AtomicLong();
    private final SecondarySemanticComponentClassifier semanticClassifier =
            new SecondarySemanticComponentClassifier();
    private final PdfSemanticNarrationSafetyValidator narrationSafety =
            new PdfSemanticNarrationSafetyValidator();
    private volatile Thread localAnalysisThread;
    private volatile String batchImageAnalysisEngineId = "";
    /** Listen skips a page already rejected safely in the current source session. */
    private final Set<Integer> fastListenRejectedPages =
            ConcurrentHashMap.newKeySet();
    /** Listen does not immediately repeat a technical failure in this session. */
    private final Set<Integer> fastListenFailedPages =
            ConcurrentHashMap.newKeySet();

    public PdfNarratablePreparationCoordinator(DocuPodcastShellViewModel viewModel,
                                               FxBackgroundTaskRunner backgroundTaskRunner,
                                               ExceptionAlertPresenter alertPresenter,
                                               Supplier<Window> ownerSupplier) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        Objects.requireNonNull(backgroundTaskRunner, "backgroundTaskRunner");
        this.alertPresenter = Objects.requireNonNull(alertPresenter, "alertPresenter");
        this.ownerSupplier = Objects.requireNonNull(ownerSupplier, "ownerSupplier");
        viewModel.currentPreparedPdfSourceProperty().addListener((observable, previous, current) ->
                resetSession(current == null ? null : current.workspace().sourcePath()));
    }

    public void prepareThenRun(Runnable continuation) {
        prepare(PdfPreparationScope.CURRENT_PAGE, 0, 0,
                PdfNarratablePreparationMode.PLAY_SELECTION, continuation);
    }

    public void prepareForwardThenRun(Runnable continuation) {
        prepare(PdfPreparationScope.PAGE_RANGE, 0, 0,
                PdfNarratablePreparationMode.GENERATE_PORTION, continuation);
    }

    /** Releases playback as soon as a page in the bounded window is narratable. */
    public void prepareFastListenThenRun(IntConsumer continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation, 0);
            return;
        }
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        // "Escuchar documento" is a document-level command: stale selection or
        // viewport state must not move its starting point away from page one.
        int anchor = 1;
        DocumentAudioPreparationExtent configured =
                viewModel.documentAudioPreparationExtent();
        int pageCount = configured.preparationPageCount() > 0
                ? configured.preparationPageCount()
                : DocumentAudioPreparationExtent.SHORT_READING.preparationPageCount();
        int end = Math.min(last, anchor + Math.max(1, pageCount) - 1);
        viewModel.beginIncrementalPdfAudio(anchor, end, true);
        prepareFastWindow(anchor, end, () -> run(continuation, anchor));
    }

    public void prepareAudioThenRun(Runnable continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation);
            return;
        }
        PreparedPdfWorkspaceRef workspace = source.workspace();
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(workspace);
        int selectedPage = viewModel.selectedPdfRegionProperty().get() == null
                ? 0 : viewModel.selectedPdfRegionProperty().get().pageNumber();
        int current = Math.max(1, selectedPage > 0
                ? selectedPage : viewModel.pdfVisiblePageNumber());
        DocumentProcessingScope activeScope = viewModel.documentProcessingScope();
        switch (activeScope) {
            case FULL_DOCUMENT -> prepare(PdfPreparationScope.PAGE_RANGE, 1, last,
                    PdfNarratablePreparationMode.GENERATE_PORTION, continuation);
            case INTERVAL -> {
                var interval = viewModel.validatedDocumentProcessingInterval()
                        .orElseThrow(() -> new IllegalStateException(
                                "El intervalo PDF activo no es valido."));
                prepare(PdfPreparationScope.PAGE_RANGE, interval.start(), interval.end(),
                        PdfNarratablePreparationMode.PROCESS_INTERVAL, continuation);
            }
            case SINGLE_FRAGMENT -> prepare(PdfPreparationScope.PAGE_RANGE,
                    current, current, PdfNarratablePreparationMode.GENERATE_PORTION,
                    continuation);
            case FROM_SELECTION -> prepare(PdfPreparationScope.PAGE_RANGE,
                    current, last, PdfNarratablePreparationMode.GENERATE_PORTION,
                    continuation);
        }
    }

    /** Prepares from the explicit/visible selection through the end of the PDF. */
    public void prepareFromSelectionThenRun(Runnable continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation);
            return;
        }
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        int selectedPage = viewModel.selectedPdfRegionProperty().get() == null
                ? 0 : viewModel.selectedPdfRegionProperty().get().pageNumber();
        int current = Math.max(1, selectedPage > 0
                ? selectedPage : viewModel.pdfVisiblePageNumber());
        prepare(PdfPreparationScope.PAGE_RANGE, current, last,
                PdfNarratablePreparationMode.GENERATE_PORTION, continuation);
    }

    public void prepareCompleteAudioThenRun(Runnable continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation);
            return;
        }
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        viewModel.beginIncrementalPdfAudio(1, last, false);
        prepare(PdfPreparationScope.PAGE_RANGE, 1, last,
                PdfNarratablePreparationMode.COMPLETE_BATCH, continuation,
                viewModel::acceptPreparedPdfPageForIncrementalAudio);
    }

    /** Prepares canonical pages/treatments only; audio remains a Generate concern. */
    public void prepareCompleteReadingThenRun(Runnable continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation);
            return;
        }
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        prepare(PdfPreparationScope.PAGE_RANGE, 1, last,
                PdfNarratablePreparationMode.COMPLETE_BATCH, continuation);
    }

    /**
     * Complete preparation for an unattended batch. It never opens a modal
     * dialog: a terminal preparation problem is returned to the queue so the
     * current document can be recorded as failed and the next one can start.
     */
    public void prepareCompleteReadingForBatchThenRun(
            Runnable continuation, Consumer<Throwable> failureHandler) {
        prepareCompleteReadingForBatchThenRun(continuation, failureHandler, "");
    }

    public void prepareCompleteReadingForBatchThenRun(
            Runnable continuation, Consumer<Throwable> failureHandler,
            String imageAnalysisEngineId) {
        Objects.requireNonNull(continuation, "continuation");
        Objects.requireNonNull(failureHandler, "failureHandler");
        batchImageAnalysisEngineId = imageAnalysisEngineId == null ? "" : imageAnalysisEngineId.strip();
        Runnable completed = () -> {
            batchImageAnalysisEngineId = "";
            continuation.run();
        };
        Consumer<Throwable> failed = failure -> {
            batchImageAnalysisEngineId = "";
            failureHandler.accept(failure);
        };
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(completed);
            return;
        }
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        prepare(PdfPreparationScope.PAGE_RANGE, 1, last,
                PdfNarratablePreparationMode.COMPLETE_BATCH, completed,
                null, true, failed);
    }

    /** Prepares exactly the validated one-based PDF page interval. */
    public void prepareIntervalThenRun(DocumentProcessingInterval interval,
                                       Runnable continuation) {
        Objects.requireNonNull(interval, "interval");
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            throw new IllegalStateException("No hay un PDF activo para procesar el intervalo.");
        }
        int pageCount = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        interval.validatedAgainst(pageCount);
        prepare(PdfPreparationScope.PAGE_RANGE, interval.start(), interval.end(),
                PdfNarratablePreparationMode.PROCESS_INTERVAL, continuation);
    }

    /**
     * Abandons lower-priority PDF work and prepares an urgent listening window
     * from the selected anchor. Completed page/treatment artifacts remain in
     * the workspace and are reused by the new plan.
     */
    public void reprioritizeFromSelectionThenRun(Runnable continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation);
            return;
        }
        int requestedStart = viewModel.selectedPdfRegionProperty().get() == null
                ? Math.max(1, viewModel.pdfVisiblePageNumber())
                : Math.max(1, viewModel.selectedPdfRegionProperty().get().pageNumber());
        cancelLocalAnalysis();
        restartSession(source.workspace().sourcePath());
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(source.workspace());
        int start = Math.min(Math.max(1, last), requestedStart);
        viewModel.updateStatusMessage(
                "Repriorizando la lectura desde la página " + start
                        + "; se conserva lo ya terminado.");
        int lookAhead = DocumentAudioPreparationExtent.SHORT_READING
                .preparationPageCount();
        prepareFastWindow(start, Math.min(last, start + lookAhead - 1),
                continuation);
    }

    public void cancelLocalAnalysis() {
        localAnalysisGeneration.incrementAndGet();
        Thread worker = localAnalysisThread;
        if (worker != null) worker.interrupt();
        viewModel.updateStatusMessage(
                "Deteniendo el análisis local; se conservan los borradores ya guardados.");
    }

    /** Cancels only work owned by the current explicit PDF preparation session. */
    public void cancelActivePreparationSession() {
        cancelLocalAnalysis();
        PdfProjectSessionToken cancelled = session;
        viewModel.projectWorkspace().document().pdfPagePreparationScheduler()
                .closeSession(cancelled);
        session = new PdfProjectSessionToken();
        viewModel.updateStatusMessage(
                "Páginas pendientes de esta preparación canceladas; se conserva lo ya publicado.");
    }

    private void prepare(
            PdfPreparationScope scope,
            int requestedStart,
            int requestedEnd,
            PdfNarratablePreparationMode mode,
            Runnable continuation) {
        prepare(scope, requestedStart, requestedEnd, mode, continuation, null);
    }

    private void prepare(
            PdfPreparationScope scope,
            int requestedStart,
            int requestedEnd,
            PdfNarratablePreparationMode mode,
            Runnable continuation,
            IntConsumer acceptedPageConsumer) {
        prepare(scope, requestedStart, requestedEnd, mode, continuation,
                acceptedPageConsumer, false, ignored -> { });
    }

    private void prepare(
            PdfPreparationScope scope,
            int requestedStart,
            int requestedEnd,
            PdfNarratablePreparationMode mode,
            Runnable continuation,
            IntConsumer acceptedPageConsumer,
            boolean unattended,
            Consumer<Throwable> failureHandler) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null) {
            run(continuation);
            return;
        }
        PreparedPdfWorkspaceRef workspace = source.workspace();
        resetSession(workspace.sourcePath());
        int current = Math.max(1, viewModel.pdfVisiblePageNumber());
        int last = viewModel.projectWorkspace().document()
                .openPreparedPdfWorkspace().pageCount(workspace);
        int start = scope == PdfPreparationScope.PAGE_RANGE
                ? (requestedStart > 0 ? requestedStart : current)
                : 0;
        int end = scope == PdfPreparationScope.PAGE_RANGE
                ? (requestedEnd >= start ? requestedEnd : last)
                : 0;
        PdfPreparationScopeRequest request = new PdfPreparationScopeRequest(
                workspace, scope, current, start, end, null,
                origin(mode), origin(mode).retryAuthority(), null);
        org.slf4j.LoggerFactory.getLogger(PdfNarratablePreparationCoordinator.class)
                .info("[PDF][SCOPE] semantic request origin={} scopeId={} retryAllowed={} mode={} pages={}-{}",
                        request.origin(), request.scopeId(), request.retryAllowed(),
                        mode, start, end);
        Path cache = viewModel.currentProjectDirectory()
                .map(root -> root.resolve("cache").resolve("pdf-ocr")).orElse(null);
        if (!mode.background()) {
            viewModel.updateStatusMessage("Preparando el alcance PDF solicitado...");
        }
        AtomicBoolean continuationStarted = new AtomicBoolean();
        PdfProjectSessionToken requestSession = session;
        if (mode == PdfNarratablePreparationMode.PROCESS_INTERVAL) {
            org.slf4j.LoggerFactory.getLogger(PdfNarratablePreparationCoordinator.class)
                    .info("[PDF][SCOPE] operationId={} sourceSha={} scope=INTERVAL startPage={} endPage={} resolvedPages={} pageCount={} origin={} retryAuthority={}",
                            request.scopeId(), workspace.sourceSha256(), start, end,
                            java.util.stream.IntStream.rangeClosed(start, end).boxed().toList(),
                            last, request.origin(), request.retryAllowed());
        }
        viewModel.projectWorkspace().document().preparePdfScope()
                .execute(request, cache, mode.priority(), requestSession,
                        progress -> {
                            boolean startNow = mode.startsWhenFirstPageIsNarratable()
                                    && pageIsNarratable(workspace, progress.currentPage())
                                    && continuationStarted.compareAndSet(false, true);
                            if (!mode.background()) {
                                Platform.runLater(() -> {
                                    String preparation = !progress.message().isBlank()
                                            ? progress.message()
                                            : progress.estimatedRemainingSeconds() < 0
                                            ? "Preparando PDF: " + progress.completedPages() + "/"
                                            + progress.totalPages() + "."
                                            : "Preparando PDF: " + progress.completedPages() + "/"
                                            + progress.totalPages() + "; faltan aproximadamente "
                                            + progress.estimatedRemainingSeconds() + " s.";
                                    if (viewModel.audioJobRunningProperty().get()) {
                                        var audioStatus = viewModel.activeAudioJobStatusProperty().get();
                                        preparation += audioStatus.stage()
                                                == com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage.WAITING_FOR_RESOURCES
                                                ? " · audio pendiente de recursos."
                                                : " · sintetizando audio de una página ya aceptada.";
                                    }
                                    viewModel.updateStatusMessage(preparation);
                                });
                            }
                            if (startNow) {
                                Platform.runLater(() -> run(continuation));
                            }
                        }, result -> {
                            if (acceptedPageConsumer != null && result.succeeded()) {
                                fastListenRejectedPages.remove(result.pageNumber());
                                fastListenFailedPages.remove(result.pageNumber());
                                Platform.runLater(() -> acceptedPageConsumer.accept(
                                        result.pageNumber()));
                            }
                        })
                .whenComplete((result, error) -> Platform.runLater(() ->
                        finish(result, error, mode, continuation,
                                continuationStarted, requestSession,
                                unattended, failureHandler)));
    }

    private void finish(PdfPreparationScopeResult result, Throwable error,
                        PdfNarratablePreparationMode mode,
                        Runnable continuation,
                        AtomicBoolean continuationStarted,
                        PdfProjectSessionToken requestSession,
                        boolean unattended,
                        Consumer<Throwable> failureHandler) {
        if (mode.background()) {
            if (error != null) {
                viewModel.updateStatusMessage("La preparación anticipada del PDF "
                        + "no pudo continuar: " + safeFailureMessage(error));
            } else if (result != null && !result.cancelled() && requestSession.active()) {
                run(continuation);
            }
            return;
        }
        if (error != null) {
            if (continuationStarted.get()) {
                viewModel.updateStatusMessage("La escucha ya comenzó, pero la preparación "
                        + "posterior encontró un error: " + safeFailureMessage(error));
            } else {
                if (unattended) {
                    failureHandler.accept(error);
                } else {
                    alertPresenter.showFailure("No se pudo preparar el PDF", error, ownerSupplier.get());
                }
            }
            return;
        }
        if (result == null || result.cancelled() || !requestSession.active()) {
            if (unattended) {
                failureHandler.accept(new IllegalStateException(
                        result != null && result.cancelled()
                                ? "La preparación PDF fue cancelada."
                                : "La preparación PDF terminó sin un resultado utilizable."));
            }
            return;
        }
        if (result.resolution().requiresManualRange()
                || result.resolution().pages().isEmpty()) {
            viewModel.updateStatusMessage(result.resolution().explanation());
            if (unattended) {
                failureHandler.accept(new IllegalStateException(
                        result.resolution().explanation()));
            }
            return;
        }
        result.pageOutcomes().stream().filter(PreparePdfPageResult::rejected)
                .map(PreparePdfPageResult::pageNumber)
                .forEach(fastListenRejectedPages::add);
        if (mode == PdfNarratablePreparationMode.FAST_LISTEN
                || mode == PdfNarratablePreparationMode.PLAY_SELECTION) {
            result.pageOutcomes().stream().filter(PreparePdfPageResult::technicalFailure)
                    .map(PreparePdfPageResult::pageNumber)
                    .forEach(fastListenFailedPages::add);
        }
        if (!continuationStarted.get()
                && !result.preparedPages().isEmpty()) viewModel.updateStatusMessage(
                "Alcance PDF preparado: " + result.preparedPages().size() + " página(s).");
        if (!result.failedPages().isEmpty()) {
            String summary = scopeResultSummary(result);
            if (result.preparedPages().isEmpty() && !continuationStarted.get()) {
                if (unattended) {
                    failureHandler.accept(new IllegalStateException(summary));
                } else {
                    alertPresenter.show(scopeResultNotification(result), ownerSupplier.get());
                }
                viewModel.updateStatusMessage(summary);
                return;
            }
            if (!unattended) {
                alertPresenter.show(scopeResultNotification(result), ownerSupplier.get());
            }
            viewModel.updateStatusMessage(summary);
        }
        if (continuationStarted.get()
                || !continuationStarted.compareAndSet(false, true)) return;
        if (!mode.requiresSecondarySemanticPreparation()) {
            run(continuation);
            return;
        }
        List<String> pending = pendingSemanticRegionIds(
                viewModel.currentPreparedPdfSourceProperty().get(),
                result.resolution().pages());
        if (!pending.isEmpty()) {
            handlePendingTechnicalElements(pending, continuation);
            return;
        }
        run(continuation);
    }

    static String scopeResultSummary(PdfPreparationScopeResult result) {
        int completed = result == null ? 0 : result.pageOutcomes().size();
        int prepared = result == null ? 0 : result.preparedPages().size();
        int rejected = result == null ? 0 : result.rejectedPages().size();
        int failed = result == null ? 0 : result.technicalFailurePages().size();
        int cancelled = result == null ? 0 : result.cancelledPages().size();
        if (rejected + failed + cancelled == 0) {
            return "Procesamiento terminado: " + completed + " página(s) procesada(s); "
                    + prepared + " preparada(s).";
        }
        StringBuilder message = new StringBuilder("Procesamiento terminado: ")
                .append(completed).append(" página(s) procesada(s). ");
        message.append("Preparadas: ").append(prepared).append('.');
        if (rejected > 0) message.append(" No interpretables: ").append(rejected).append('.');
        if (failed > 0) message.append(" Fallos técnicos: ").append(failed).append('.');
        if (cancelled > 0) message.append(" Canceladas: ").append(cancelled).append('.');
        return message.toString();
    }

    private static PdfPreparationOrigin origin(PdfNarratablePreparationMode mode) {
        return switch (mode) {
            case FAST_LISTEN -> PdfPreparationOrigin.LISTEN_DOCUMENT;
            case PLAY_SELECTION -> PdfPreparationOrigin.LISTEN_FROM_HERE;
            case GENERATE_PORTION -> PdfPreparationOrigin.PROCESS_FROM_HERE;
            case PROCESS_INTERVAL -> PdfPreparationOrigin.PROCESS_INTERVAL;
            case COMPLETE_BATCH -> PdfPreparationOrigin.PROCESS_COMPLETE;
            case BACKGROUND_LOOK_AHEAD -> PdfPreparationOrigin.ACTIVE_JOB_PREFETCH;
        };
    }

    static UserNotification scopeResultNotification(PdfPreparationScopeResult result) {
        String headline = result.aggregateStatus()
                == PdfPreparationScopeResult.AggregateStatus.PARTIAL_SUCCESS
                ? "Preparación completada con incidencias"
                : "No se pudieron preparar las páginas solicitadas";
        boolean hasTechnical = !result.technicalFailurePages().isEmpty();
        StringBuilder affected = new StringBuilder(scopeResultSummary(result));
        for (PreparePdfPageResult outcome : result.pageOutcomes()) {
            if (outcome.succeeded()) continue;
            affected.append(System.lineSeparator()).append("Página ")
                    .append(outcome.pageNumber()).append(" — ")
                    .append(outcome.userFacingReason().isBlank()
                            ? outcome.terminalMessage() : outcome.userFacingReason());
            if (outcome.canonicalPageAvailable()) {
                affected.append(" Se conserva una versión preparada anterior.");
            }
        }
        return new UserNotification(
                hasTechnical ? UserNotificationLevel.WARNING
                        : UserNotificationLevel.INFORMATION,
                "DocuPodcast Studio", headline, affected.toString(),
                scopeTechnicalDetail(result));
    }

    private static String scopeTechnicalDetail(PdfPreparationScopeResult result) {
        StringBuilder detail = new StringBuilder();
        for (PreparePdfPageResult outcome : result.pageOutcomes()) {
            if (outcome.succeeded()) continue;
            if (!detail.isEmpty()) detail.append(System.lineSeparator());
            detail.append("page=").append(outcome.pageNumber())
                    .append(" outcome=").append(outcome.outcomeState())
                    .append(" category=").append(outcome.failureCategory().isBlank()
                            ? "none" : outcome.failureCategory())
                    .append(" canonicalPage=")
                    .append(outcome.canonicalPageAvailable())
                    .append(" finalStage=").append(finalStage(outcome));
            if (outcome.preparedPage() != null) {
                detail.append(" durationMs=")
                        .append(outcome.preparedPage().preparationMetrics()
                                .elapsedMillis())
                        .append(" verifier=").append(hasSemanticPass(
                                outcome, "verifier"))
                        .append(" recovery=").append(hasSemanticPass(
                                outcome, "recovery"));
            }
            Throwable cause = outcome.diagnosticCause();
            if (cause != null) detail.append(" cause=")
                    .append(cause.getClass().getName()).append(": ")
                    .append(Objects.toString(cause.getMessage(), ""));
        }
        return detail.toString();
    }

    private static String finalStage(PreparePdfPageResult outcome) {
        return switch (outcome.outcomeState()) {
            case COMPLETED -> "PUBLISHED";
            case INSUFFICIENT_EVIDENCE -> "SEMANTIC_VALIDATION";
            case TRUNCATED -> "MODEL_OUTPUT";
            case FAILED -> "TECHNICAL_FAILURE";
            case CANCELLED -> "CANCELLED";
            case INTERRUPTED -> "INTERRUPTED";
            case IN_FLIGHT -> "IN_FLIGHT";
        };
    }

    private static boolean hasSemanticPass(PreparePdfPageResult outcome,
                                           String pass) {
        return outcome.preparedPage().regions().stream().anyMatch(region ->
                pass.equalsIgnoreCase(region.attributes().getOrDefault(
                        "semanticPass", "")));
    }

    private void prepareFastWindow(int anchor, int end, Runnable continuation) {
        prepareFastCandidate(anchor, end, continuation);
    }

    private void prepareFastCandidate(int candidate, int end,
                                      Runnable continuation) {
        if (fastListenFailedPages.contains(candidate)) {
            viewModel.updateStatusMessage(fastListenSkipMessage(
                    candidate, true, false));
            if (candidate < end) prepareFastCandidate(candidate + 1, end, continuation);
            else run(continuation);
            return;
        }
        if (fastListenRejectedPages.contains(candidate)) {
            viewModel.updateStatusMessage(fastListenSkipMessage(
                    candidate, false, true));
            if (candidate < end) prepareFastCandidate(candidate + 1, end, continuation);
            else run(continuation);
            return;
        }
        prepare(PdfPreparationScope.PAGE_RANGE, candidate, candidate,
                PdfNarratablePreparationMode.FAST_LISTEN, () -> {
                    PreparedPdfSource source = viewModel
                            .currentPreparedPdfSourceProperty().get();
                    boolean narratable = source != null && pageIsNarratable(
                            source.workspace(), candidate);
                    if (!narratable && candidate < end) {
                        prepareFastCandidate(candidate + 1, end, continuation);
                        return;
                    }
                    run(continuation);
                    if (candidate < end) {
                        Platform.runLater(() -> prepare(
                                PdfPreparationScope.PAGE_RANGE,
                                candidate + 1, end,
                                PdfNarratablePreparationMode.BACKGROUND_LOOK_AHEAD,
                                viewModel::markIncrementalPdfPreparationComplete,
                                viewModel::acceptPreparedPdfPageForIncrementalAudio));
                    } else {
                        viewModel.markIncrementalPdfPreparationComplete();
                    }
                }, viewModel::acceptPreparedPdfPageForIncrementalAudio);
    }

    static String fastListenSkipMessage(int page, boolean failed,
                                        boolean rejected) {
        if (failed) {
            return "La página " + page
                    + " se omitió: ya tuvo un fallo técnico en esta sesión. "
                    + "Usa Procesar o Reintentar para intentarlo expresamente.";
        }
        if (rejected) {
            return "La página " + page
                    + " se omitió: el último análisis de esta sesión no tuvo "
                    + "evidencia suficiente. Usa Procesar para reintentar manualmente.";
        }
        return "";
    }

    private boolean pageIsNarratable(PreparedPdfWorkspaceRef workspace,
                                     int page) {
        if (workspace == null || page <= 0) return false;
        try {
            var repository = viewModel.projectWorkspace().document()
                    .openPreparedPdfWorkspace();
            return repository.pagePrepared(workspace, page)
                    && repository.narratableRegionCount(workspace, page) > 0;
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private int selectedOrVisiblePage() {
        PdfRegionSelectionRef selected = viewModel.selectedPdfRegionProperty().get();
        return Math.max(1, selected == null
                ? viewModel.pdfVisiblePageNumber() : selected.pageNumber());
    }

    private List<String> pendingSemanticRegionIds(
            PreparedPdfSource source, List<Integer> pages) {
        SecondarySemanticReadingPolicy policy = viewModel
                .documentListeningPreferences().secondarySemanticPolicy();
        if (source == null || pages == null || pages.isEmpty()
                || policy == SecondarySemanticReadingPolicy.OMIT_ALL) {
            return List.of();
        }
        ArrayList<String> pending = new ArrayList<>();
        for (int page : pages) {
            try {
                for (DocumentTechnicalElement element : viewModel.projectWorkspace()
                        .document().resolveTechnicalElements().resolveAll(
                                source.workspace().projectRoot(), page)) {
                    if (!policy.includes(
                            semanticClassifier.classify(element.type()))) continue;
                    boolean described = element.sourceRegionIds().stream()
                            .allMatch(regionId -> hasEffectiveDescription(
                                    source, page, regionId));
                    if (!described) pending.addAll(element.sourceRegionIds());
                }
            } catch (Exception failure) {
                viewModel.updateStatusMessage(
                        "No se pudieron enumerar los componentes de la página "
                                + page + ".");
            }
        }
        return pending.stream().distinct().toList();
    }

    private boolean hasEffectiveDescription(
            PreparedPdfSource source, int page, String regionId) {
        try {
            return viewModel.projectWorkspace().document()
                    .listPdfDerivedTreatments().forRegion(
                            source.workspace().projectRoot(), page, regionId)
                    .stream().anyMatch(this::isEffectiveTechnicalDescription);
        } catch (Exception failure) {
            return false;
        }
    }

    private boolean isEffectiveTechnicalDescription(
            PdfDerivedTreatment treatment) {
        if (treatment == null
                || treatment.derivedText() == null
                || treatment.derivedText().isBlank()) {
            return false;
        }
        boolean narratableState = treatment.state() == PdfDerivedTreatmentState.APPROVED
                ? narrationSafety.safeApprovedText(treatment)
                : treatment.state() == PdfDerivedTreatmentState.DRAFT
                && DocumentListeningPreferences.QUALITY_MODEL.equals(treatment.modelId())
                && narrationSafety.safeForAutomaticAdmission(treatment);
        return narratableState && switch (treatment.kind()) {
            case IMAGE_DESCRIPTION, MATHEMATICAL_READING,
                    TABLE_NARRATION, SMALL_TABLE_NARRATION,
                    LIGHTWEIGHT_LANGUAGE_MODEL -> true;
            default -> false;
        };
    }

    private void handlePendingTechnicalElements(
            List<String> pending, Runnable continuation) {
        reviewWithLocalAiThenRun(pending, continuation);
    }

    private void reviewWithLocalAiThenRun(List<String> regionIds,
                                          Runnable continuation) {
        PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
        if (source == null || regionIds == null || regionIds.isEmpty()) {
            run(continuation);
            return;
        }
        List<String> requested = regionIds.stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
        viewModel.beginLocalDocumentAnalysis(
                "Revisando contenido dudoso",
                "Preparando modelo local…");
        long generation = localAnalysisGeneration.incrementAndGet();
        localAnalysisThread = Thread.ofVirtual()
                .name("pdf-review-doubtful-content").start(() -> {
            ArrayList<String> warnings = new ArrayList<>();
            try {
                viewModel.projectWorkspace().document().runLocalAnalysisBatch()
                        .execute("document-listening-analysis-batch",
                                () -> analysisCancelled(generation),
                                (stage, progress, message) -> { },
                                () -> {
                                    analyzeBatch(source, requested, warnings,
                                            generation);
                                    return null;
                                });
                if (analysisCancelled(generation)) {
                    Platform.runLater(() -> {
                        viewModel.endLocalDocumentAnalysis();
                        viewModel.updateStatusMessage(
                                "Análisis local detenido. Los borradores guardados se conservaron.");
                    });
                    return;
                }
                Platform.runLater(() -> {
                    viewModel.endLocalDocumentAnalysis();
                    viewModel.updateStatusMessage(warnings.isEmpty()
                            ? "Lectura semántica preparada con el Modelo de IA y validación de voz."
                            : "Revisión local terminada con "
                            + warnings.size() + " advertencia(s).");
                    run(continuation);
                });
            } catch (InterruptedException cancelled) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    viewModel.endLocalDocumentAnalysis();
                    viewModel.updateStatusMessage(
                            "Análisis local detenido. No se inició la generación de voz.");
                });
            } catch (Throwable failure) {
                Platform.runLater(() -> {
                    viewModel.endLocalDocumentAnalysis();
                    viewModel.updateStatusMessage(
                            "La revisión local no pudo completarse; se continuará "
                                    + "omitiendo los elementos pendientes. "
                                    + safeFailureMessage(failure));
                    run(continuation);
                });
            } finally {
                if (localAnalysisThread == Thread.currentThread()) {
                    localAnalysisThread = null;
                }
            }
        });
    }

    private void analyzeBatch(
            PreparedPdfSource source, List<String> requested,
            List<String> warnings, long generation)
            throws java.io.IOException, InterruptedException {
        Map<Integer, List<String>> byPage = groupByPage(requested);
        ArrayList<PageElements> pages = new ArrayList<>();
        int total = 0;
        for (var pageEntry : byPage.entrySet()) {
            List<DocumentTechnicalElement> elements =
                    viewModel.projectWorkspace().document()
                            .resolveTechnicalElements().resolve(
                                    source.workspace().projectRoot(),
                                    pageEntry.getKey(), pageEntry.getValue());
            pages.add(new PageElements(
                    pageEntry.getKey(), pageEntry.getValue(), elements));
            total += elements.size();
        }
        int completed = 0;
        for (PageElements page : pages) {
            if (analysisCancelled(generation)) throw new InterruptedException();
            for (DocumentTechnicalElement element : page.elements()) {
                if (analysisCancelled(generation)) throw new InterruptedException();
                int current = completed + 1;
                viewModel.updateLocalDocumentAnalysisProgress(
                        completed, total, "Analizando elemento técnico "
                                + current + " de " + total + ".");
                try {
                    generateTechnicalDescriptionIfUseful(source, element);
                } catch (InterruptedException cancelled) {
                    throw cancelled;
                } catch (Exception lastFailure) {
                    warnings.add(element.id() + ": "
                            + safeFailureMessage(lastFailure));
                }
                completed++;
                viewModel.updateLocalDocumentAnalysisProgress(
                        completed, total, "Guardando descripción "
                                + completed + " de " + total + ".");
            }
        }
        viewModel.beginLocalDocumentAnalysis(
                "Revisando contenido dudoso",
                "Liberando memoria para la voz…");
    }

    private void generateTechnicalDescriptionIfUseful(
            PreparedPdfSource source, DocumentTechnicalElement element)
            throws Exception {
        int pageNumber = element.pageNumber();
        String regionId = element.anchorRegionId();
        PdfRegionSelectionRef selection = new PdfRegionSelectionRef(
                pageNumber, regionId, 0, Integer.MAX_VALUE);
        DocumentSelectionSnapshot snapshot = viewModel.projectWorkspace()
                .document().resolveDocumentSelection()
                .resolve(source, selection).orElse(null);
        if (snapshot == null || snapshot.geometry() == null) return;
        SecondarySemanticComponentKind componentKind =
                semanticClassifier.classify(element.type());
        SecondarySemanticReadingPolicy policy = viewModel
                .documentListeningPreferences().secondarySemanticPolicy();
        if (!policy.includes(componentKind)) return;
        PdfDerivedTreatmentKind kind = switch (componentKind) {
            case EQUATION -> PdfDerivedTreatmentKind.MATHEMATICAL_READING;
            case TABLE -> PdfDerivedTreatmentKind.TABLE_NARRATION;
            case IMAGE, EXTRA -> PdfDerivedTreatmentKind.IMAGE_DESCRIPTION;
            case NONE -> null;
        };
        if (kind == null) return;

        PreparedPdfRegionContext context = viewModel.projectWorkspace()
                .document().buildPreparedPdfRegionContext()
                .build(source.workspace(), selection, 3).orElse(null);
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("language", sourceLanguage(snapshot.text()));
        options.put("nearbyContext",
                element.nearbyContext().isBlank()
                        ? contextText(context, snapshot.text())
                        : element.nearbyContext());
        options.put("caption", element.caption());
        options.put("recognizedLabels", snapshot.text());
        options.put("objectId", element.id());
        options.put("componentType", element.type().name());
        options.put("componentGeometry", element.roi().bbox());
        options.put("evidenceRegionIds", String.join(",", element.sourceRegionIds()));
        options.put("semanticKind", componentKind.name());
        options.put("semanticPolicy", policy.name());
        options.put("forceRegenerate", "false");
        options.put("computePriority", "INTERACTIVE_ANALYSIS");
        options.put("model", DocumentListeningPreferences.QUALITY_MODEL);
        options.put("includeContextPage", "true");
        if (!batchImageAnalysisEngineId.isBlank()) {
            options.put("analysisEngineId", batchImageAnalysisEngineId);
        }
        options.put("maxOutputTokens",
                kind == PdfDerivedTreatmentKind.MATHEMATICAL_READING
                        ? "240"
                        : kind == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION ? "320" : "260");
        options.put("instruction", semanticInstruction(kind));

        try (PdfAnalysisVisualEvidence visuals = viewModel.projectWorkspace()
                .document().capturePdfAnalysisVisualEvidence()
                .capture(source.sourcePath(), element.roi())) {
            options.put("roiImage", visuals.roiImage().toString());
            options.put("contextImage", visuals.markedPageImage().toString());
            String engine = switch (kind) {
                case MATHEMATICAL_READING -> TransversalMathPdfTreatmentEngine.ID;
                case TABLE_NARRATION -> TransversalTableExplanationPdfTreatmentEngine.ID;
                default -> TransversalVisualPdfTreatmentEngine.ID;
            };
            PdfDerivedTreatment generated = viewModel.projectWorkspace()
                    .document().generatePdfDerivedTreatment().execute(
                            new PdfDerivedTreatmentGenerationRequest(
                                    source.workspace().projectRoot(), pageNumber,
                                    kind, element.sourceRegionIds(),
                                    engine, true, options));
            LinkedHashMap<String, String> metadata =
                    new LinkedHashMap<>(generated.metadata());
            metadata.put("technicalElementId", element.id());
            metadata.put("technicalElementType", element.type().name());
            metadata.put("anchorRegionId", element.anchorRegionId());
            metadata.put("focusBounds", element.roi().bbox());
            metadata.put("semanticPolicy", policy.name());
            metadata.put("semanticComponentKind", componentKind.name());
            PdfDerivedTreatment description = new PdfDerivedTreatment(
                    generated.id(), generated.kind(),
                    generated.sourceRegionIds(), generated.derivedText(),
                    generated.modelId(), generated.modelVersion(),
                    generated.confidence(), generated.createdAt(),
                    PdfDerivedTreatmentState.DRAFT, generated.sourceRevision(),
                    generated.sourceFingerprint(), generated.prompt(),
                    metadata);
            viewModel.projectWorkspace().document()
                    .updatePdfDerivedTreatment().upsert(
                            source.workspace().projectRoot(), pageNumber,
                            description);
        }
    }

    private static String semanticInstruction(PdfDerivedTreatmentKind kind) {
        String evidence = "Usa el texto cercano, la leyenda, las etiquetas, las celdas y el OCR "
                + "solo como evidencias identificadas. No resumas la página, no inventes datos "
                + "y devuelve INSUFFICIENT_EVIDENCE si no basta. ";
        if (kind == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION) {
            return "Interpreta exclusivamente el componente visual delimitado por el ROI. "
                    + evidence
                    + "Escribe de dos a cuatro frases naturales en español, idealmente entre "
                    + "45 y 90 palabras cuando la evidencia lo permita. Describe qué muestra, "
                    + "las relaciones visuales importantes, las etiquetas o leyenda y por qué "
                    + "el elemento importa dentro de la explicación.";
        }
        return "Interpreta exclusivamente el componente delimitado por el ROI. " + evidence;
    }

    private record PageElements(
            int pageNumber,
            List<String> regionIds,
            List<DocumentTechnicalElement> elements) {
    }

    private static Map<Integer, List<String>> groupByPage(List<String> ids) {
        Map<Integer, List<String>> grouped = new TreeMap<>();
        for (String id : ids) {
            Matcher matcher = REGION_PAGE.matcher(id);
            if (!matcher.find()) {
                throw new IllegalArgumentException(
                        "No se pudo identificar la página de la región " + id + ".");
            }
            int page = Integer.parseInt(matcher.group(1));
            grouped.computeIfAbsent(page, ignored -> new ArrayList<>()).add(id);
        }
        return grouped;
    }

    private static PdfDerivedTreatmentKind treatmentKind(
            PdfRegionType type, String reason) {
        if (type == PdfRegionType.MATH || "FORMULA_FRAGMENT".equals(reason)) {
            return PdfDerivedTreatmentKind.MATHEMATICAL_READING;
        }
        if (type == PdfRegionType.IMAGE || type == PdfRegionType.CAPTION
                || type == PdfRegionType.TABLE
                || "TABLE_FRAGMENT".equals(reason)
                || "AMBIGUOUS".equals(reason)
                || "OTHER".equals(reason)) {
            return PdfDerivedTreatmentKind.IMAGE_DESCRIPTION;
        }
        return null;
    }

    private static PdfRegionType pdfRegionType(String value) {
        try {
            return PdfRegionType.valueOf(
                    value == null ? "UNKNOWN" : value.strip().toUpperCase());
        } catch (IllegalArgumentException invalid) {
            return PdfRegionType.UNKNOWN;
        }
    }

    private static String contextText(PreparedPdfRegionContext context,
                                      String selected) {
        if (context == null) return selected == null ? "" : selected;
        StringBuilder value = new StringBuilder(
                "CONTEXTO ORIENTATIVO; NO ES EVIDENCIA VISUAL.\n");
        context.before().stream().skip(Math.max(0, context.before().size() - 3L))
                .forEach(item -> value.append("ANTERIOR: ")
                        .append(item.text()).append('\n'));
        context.after().stream().limit(1).forEach(item -> value
                .append("POSTERIOR: ").append(item.text()).append('\n'));
        return value.toString().strip();
    }

    private static String sourceLanguage(String text) {
        String value = text == null ? "" : text.toLowerCase(
                java.util.Locale.ROOT);
        return value.matches(
                ".*\\b(el|la|de|que|para|con|una|los|las)\\b.*")
                ? "es" : "en";
    }

    private static String safeFailureMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName() : message.strip();
    }

    private void resetSession(Path source) {
        Path normalized = source == null ? null : source.toAbsolutePath().normalize();
        if (Objects.equals(sessionSource, normalized)) return;
        viewModel.projectWorkspace().document().pdfPagePreparationScheduler().closeSession(session);
        session = new PdfProjectSessionToken();
        sessionSource = normalized;
        fastListenRejectedPages.clear();
        fastListenFailedPages.clear();
    }

    private void restartSession(Path source) {
        viewModel.projectWorkspace().document().pdfPagePreparationScheduler()
                .closeSession(session);
        session = new PdfProjectSessionToken();
        sessionSource = source == null ? null : source.toAbsolutePath().normalize();
        fastListenRejectedPages.clear();
        fastListenFailedPages.clear();
    }

    private boolean analysisCancelled(long generation) {
        return generation != localAnalysisGeneration.get()
                || Thread.currentThread().isInterrupted();
    }

    private static void run(Runnable continuation) {
        if (continuation != null) continuation.run();
    }

    private static void run(IntConsumer continuation, int page) {
        if (continuation != null) continuation.accept(page);
    }

}
