package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPagePreparationScheduler;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationPriority;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationProgress;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScope;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScopeRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScopeResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationTaskKey;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfProjectSessionToken;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfPageRequest;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfPageResult;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparePdfScopeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.OpenPreparedPdfWorkspaceUseCase;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Viewport adapter over the application-level single-worker PDF scheduler. */
final class PdfVisibleTextPreparationCoordinator {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
            PdfVisibleTextPreparationCoordinator.class);

    private final DocuPodcastShellViewModel viewModel;
    private final Supplier<Path> cacheDirectorySupplier;
    private final PreparePdfScopeUseCase prepareScope;
    private final PdfPagePreparationScheduler scheduler;
    private final OpenPreparedPdfWorkspaceUseCase openWorkspace;
    private final Runnable onPrepared;
    private final StringProperty status = new SimpleStringProperty("Preparación PDF inactiva.");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final BooleanProperty active = new SimpleBooleanProperty(false);
    private Path sessionSource;
    private PdfProjectSessionToken session = new PdfProjectSessionToken();
    private int lastVisiblePage;

    PdfVisibleTextPreparationCoordinator(DocuPodcastShellViewModel viewModel,
                                         Supplier<Path> cacheDirectorySupplier,
                                         Runnable onPrepared) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.cacheDirectorySupplier = Objects.requireNonNull(cacheDirectorySupplier, "cacheDirectorySupplier");
        this.prepareScope = viewModel.projectWorkspace().document().preparePdfScope();
        this.scheduler = viewModel.projectWorkspace().document().pdfPagePreparationScheduler();
        this.openWorkspace = viewModel.projectWorkspace().document().openPreparedPdfWorkspace();
        this.onPrepared = onPrepared == null ? () -> { } : onPrepared;
        this.scheduler.addProgressListener(this::acceptProgress);
    }

    /** Updates viewer state and may promote already queued work; never enqueues work. */
    void observeVisiblePage(PreparedPdfSource source, int visiblePage) {
        if (source == null || visiblePage <= 0) return;
        resetIfSourceChanged(source.sourcePath());
        int previous = lastVisiblePage;
        lastVisiblePage = visiblePage;
        PreparedPdfWorkspaceRef workspace = source.workspace();
        LOGGER.info("[PDF][UI] page-visible old={} new={} cause=NAVIGATION",
                previous, visiblePage);
        com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageViewerState viewerState =
                openWorkspace.pageViewerState(workspace, visiblePage);
        boolean reprioritized = scheduler.reprioritizePending(
                new PdfPreparationTaskKey(workspace.sourceSha256(), visiblePage),
                PdfPreparationPriority.URGENT);
        LOGGER.info("[PDF][P{}] viewer canonical={} action={} origin=NAVIGATION",
                visiblePage, viewerState,
                reprioritized ? "ACTIVE_JOB_REPRIORITIZED" : "DISPLAY_ONLY");
        if (viewerState != com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfPageViewerState.PREPARED && !reprioritized) {
            LOGGER.info("[PDF][P{}] semantic action=SKIPPED reason=NAVIGATION_IS_PASSIVE",
                    visiblePage);
        }
    }

    void preparePageNow(PreparedPdfSource source, int page) {
        if (source == null || page <= 0) return;
        resetIfSourceChanged(source.sourcePath());
        submit(source.workspace(), page, PdfPreparationPriority.URGENT, false,
                PdfPreparationOrigin.EXPLICIT_RETRY, true,
                "explicit-page-" + java.util.UUID.randomUUID());
    }

    static PreparePdfPageRequest explicitSemanticRetryRequest(
            PreparedPdfWorkspaceRef workspace, Path cacheDirectory, int page,
            PdfPreparationPriority priority, String scopeId) {
        return new PreparePdfPageRequest(workspace, cacheDirectory, page,
                false, null, priority,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                PdfPreparationOrigin.EXPLICIT_RETRY, true, scopeId);
    }

    void pause() {
        scheduler.pause();
    }

    void resume() {
        scheduler.resume();
    }

    void cancelPending() {
        scheduler.cancelPending(session);
    }

    void closeProject() {
        scheduler.closeSession(session);
        session = new PdfProjectSessionToken();
        sessionSource = null;
        lastVisiblePage = 0;
    }

    void prepareScope(PreparedPdfSource source,
                      PdfPreparationScope scope,
                      int rangeStart,
                      int rangeEnd,
                      Runnable onCompleted,
                      Consumer<String> onStatus) {
        if (source == null) {
            if (onStatus != null) onStatus.accept("No hay un PDF activo.");
            return;
        }
        resetIfSourceChanged(source.sourcePath());
        PreparedPdfWorkspaceRef workspace = source.workspace();
        int visiblePage = Math.max(1, viewModel.pdfVisiblePageNumber());
        PdfPreparationScopeRequest request = new PdfPreparationScopeRequest(
                workspace,
                scope,
                visiblePage,
                rangeStart,
                rangeEnd,
                viewModel.projectWorkspace().document().buildPdfEnhancedOutline().build(workspace),
                scope == PdfPreparationScope.WHOLE_DOCUMENT
                        ? PdfPreparationOrigin.PROCESS_COMPLETE
                        : PdfPreparationOrigin.PROCESS_FROM_HERE,
                true, null);
        LOGGER.info("[PDF][SCOPE] semantic request origin={} scopeId={} retryAllowed={} scope={}",
                request.origin(), request.scopeId(), request.retryAllowed(), scope);
        PdfPreparationPriority priority = scope == PdfPreparationScope.WHOLE_DOCUMENT
                ? PdfPreparationPriority.BACKGROUND
                : PdfPreparationPriority.URGENT;
        PdfProjectSessionToken submittedSession = session;
        prepareScope.execute(request, cacheDirectorySupplier.get(), priority, submittedSession, snapshot -> {
            if (onStatus != null && submittedSession.active() && submittedSession.equals(session)) {
                Platform.runLater(() -> onStatus.accept(scopeProgressLabel(snapshot)));
            }
        }).whenComplete((result, error) -> Platform.runLater(() ->
                completeScope(submittedSession, result, error, onCompleted, onStatus)));
    }

    ReadOnlyStringProperty statusProperty() {
        return status;
    }

    ReadOnlyDoubleProperty progressProperty() {
        return progress;
    }

    ReadOnlyIntegerProperty currentPageProperty() {
        return currentPage;
    }

    ReadOnlyBooleanProperty activeProperty() {
        return active;
    }

    private void submit(PreparedPdfWorkspaceRef workspace,
                        int page,
                        PdfPreparationPriority priority,
                        boolean forceOcr,
                        PdfPreparationOrigin origin,
                        boolean retryAllowed,
                        String scopeId) {
        if (page <= 0 || page > openWorkspace.pageCount(workspace)
                || (!forceOcr && openWorkspace.pagePrepared(workspace, page))) return;
        String sourceSha = workspace.sourceSha256();
        PdfProjectSessionToken submittedSession = session;
        PreparePdfPageRequest request = origin == PdfPreparationOrigin.EXPLICIT_RETRY
                && !forceOcr
                ? explicitSemanticRetryRequest(workspace,
                cacheDirectorySupplier.get(), page, priority, scopeId)
                : new PreparePdfPageRequest(workspace, cacheDirectorySupplier.get(), page,
                forceOcr, null, priority,
                com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink.NONE,
                origin, retryAllowed, scopeId);
        scheduler.submit(new PdfPreparationTaskKey(sourceSha, page), priority, submittedSession,
                        request)
                .whenComplete((result, error) -> Platform.runLater(() ->
                        complete(submittedSession, result, error)));
    }

    private void complete(PdfProjectSessionToken submittedSession,
                          PreparePdfPageResult result,
                          Throwable error) {
        if (!submittedSession.active() || !submittedSession.equals(session)) return;
        if (error != null) {
            String message = error.getMessage() == null ? "error desconocido" : error.getMessage();
            viewModel.updateStatusMessage("No se pudo preparar la página PDF: " + message);
            return;
        }
        if (result == null || result.cancelled()) return;
        if (result.succeeded()) {
            PreparedPdfSource source = viewModel.currentPreparedPdfSourceProperty().get();
            PreparedPdfWorkspaceRef current = source == null ? null : source.workspace();
            if (current == null) return;
            int narratable = openWorkspace.narratableRegionCount(current, result.pageNumber());
            viewModel.updateStatusMessage("Página PDF " + result.pageNumber() + " preparada: "
                    + narratable + " regiones narrables.");
            onPrepared.run();
        } else {
            String message = result.userFacingReason().isBlank()
                    ? result.terminalMessage() : result.userFacingReason();
            if (result.canonicalPageAvailable()) {
                message += " Se conserva la versión preparada anterior.";
            }
            viewModel.updateStatusMessage(message);
        }
    }

    private void completeScope(PdfProjectSessionToken submittedSession,
                               PdfPreparationScopeResult result,
                               Throwable error,
                               Runnable onCompleted,
                               Consumer<String> onStatus) {
        if (!submittedSession.active() || !submittedSession.equals(session)) return;
        if (error != null) {
            if (onStatus != null) onStatus.accept("No se pudo ampliar la búsqueda: " + diagnostic(error));
            return;
        }
        if (result == null || result.cancelled()) {
            if (onStatus != null) onStatus.accept("Preparación cancelada.");
            return;
        }
        if (result.resolution().requiresManualRange()) {
            if (onStatus != null) onStatus.accept(result.resolution().explanation());
            return;
        }
        if (!result.preparedPages().isEmpty()) onPrepared.run();
        if (onStatus != null) {
            String message = result.failedPages().isEmpty()
                    ? "Preparadas " + result.preparedPages().size() + " página(s)."
                    : "Procesadas " + result.pageOutcomes().size() + " página(s): "
                    + result.preparedPages().size() + " preparada(s), "
                    + result.rejectedPages().size() + " no interpretable(s), "
                    + result.technicalFailurePages().size() + " con fallo técnico.";
            onStatus.accept(message);
        }
        if (onCompleted != null) onCompleted.run();
    }

    private static String scopeProgressLabel(
            com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationScopeProgress snapshot) {
        String estimate = snapshot.estimatedRemainingSeconds() < 0
                ? ""
                : " · aprox. " + snapshot.estimatedRemainingSeconds() + " s restantes";
        return "Preparando página " + snapshot.currentPage() + " · "
                + snapshot.completedPages() + "/" + snapshot.totalPages() + estimate;
    }

    private static String diagnostic(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private void acceptProgress(PdfPreparationProgress snapshot) {
        Platform.runLater(() -> {
            status.set(snapshot.message().isBlank() ? label(snapshot) : snapshot.message());
            progress.set(snapshot.fraction());
            currentPage.set(snapshot.currentPage());
            active.set(snapshot.state() == PdfPreparationProgress.State.RUNNING
                    || snapshot.state() == PdfPreparationProgress.State.PAUSED
                    || snapshot.queued() > 0);
        });
    }

    private static String label(PdfPreparationProgress progress) {
        return switch (progress.state()) {
            case IDLE -> "Preparación PDF al día.";
            case RUNNING -> "Preparando página " + progress.currentPage() + ".";
            case PAUSED -> "Preparación PDF pausada.";
            case CANCELLED -> "Preparación PDF cancelada.";
            case FAILED -> "La preparación PDF encontró un error.";
        };
    }

    private void resetIfSourceChanged(Path sourcePath) {
        Path normalized = sourcePath == null ? null : sourcePath.toAbsolutePath().normalize();
        if (Objects.equals(sessionSource, normalized)) return;
        scheduler.closeSession(session);
        session = new PdfProjectSessionToken();
        sessionSource = normalized;
        lastVisiblePage = 0;
    }

}
