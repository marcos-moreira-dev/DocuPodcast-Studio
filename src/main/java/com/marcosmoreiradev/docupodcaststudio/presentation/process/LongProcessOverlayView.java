package com.marcosmoreiradev.docupodcaststudio.presentation.process;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationProgress;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Floating overlay for long-running document operations.
 *
 * <p>Audio jobs are generated outside JavaFX. This overlay only observes progress and must never
 * scan files or perform heavy work on the JavaFX application thread.</p>
 */
public final class LongProcessOverlayView extends VBox {
    private static final long SIZE_PROBE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(3);
    private static final ExecutorService SIZE_PROBE_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "docupodcast-audio-size-probe");
        thread.setDaemon(true);
        return thread;
    });

    private final DocuPodcastShellViewModel viewModel;
    private final Label title = new Label();
    private final Label detail = new Label();
    private final Label eta = new Label();
    private final ProgressBar progress = StudioFeedbackControls.progressBar(0);
    private final Label progressPercent = new Label("0 %");
    private final VBox preparationCard = new VBox(6);
    private final Label preparationTitle = new Label("Preparación del documento");
    private final Label preparationStatus = new Label();
    private final ProgressBar preparationProgress = StudioFeedbackControls.progressBar(0);
    private final Label preparationPercent = new Label("0 %");
    private final Button pausePreparation;
    private final Button resumePreparation;
    private final Button cancelPendingPreparation;
    private final BooleanProperty expanded;
    private final BooleanProperty maximized;
    private final Button hide;
    private final Button maximize;
    private final Button restoreHalf;
    private final Button cancel;
    private final Runnable cancelLocalAnalysis;
    private final AtomicBoolean sizeProbeRunning = new AtomicBoolean(false);
    private volatile String sizeProbeKey = "";
    private volatile String cachedSizeLabel = "audio generado en disco: calculando";
    private volatile long lastSizeProbeNanos = 0L;

    public LongProcessOverlayView(
            DocuPodcastShellViewModel viewModel,
            BooleanProperty expanded,
            Runnable cancelLocalAnalysis) {
        this(viewModel, expanded, cancelLocalAnalysis,
                viewModel::cancelPendingDocumentPreparation,
                new javafx.beans.property.SimpleBooleanProperty(false));
    }

    public LongProcessOverlayView(
            DocuPodcastShellViewModel viewModel,
            BooleanProperty expanded,
            Runnable cancelLocalAnalysis,
            Runnable cancelPendingPreparationAction) {
        this(viewModel, expanded, cancelLocalAnalysis, cancelPendingPreparationAction,
                new javafx.beans.property.SimpleBooleanProperty(false));
    }

    public LongProcessOverlayView(
            DocuPodcastShellViewModel viewModel,
            BooleanProperty expanded,
            Runnable cancelLocalAnalysis,
            Runnable cancelPendingPreparationAction,
            BooleanProperty maximized) {
        this.viewModel = viewModel;
        this.expanded = expanded;
        this.maximized = maximized == null
                ? new javafx.beans.property.SimpleBooleanProperty(false) : maximized;
        this.cancelLocalAnalysis = cancelLocalAnalysis == null
                ? () -> { } : cancelLocalAnalysis;
        this.hide = ActionButtonFactory.secondary("Ocultar", () -> this.expanded.set(false));
        this.maximize = ActionButtonFactory.secondary("▱",
                "Maximizar el panel de trabajo pesado", () -> this.maximized.set(true));
        this.restoreHalf = ActionButtonFactory.secondary("▭",
                "Restaurar el panel a la mitad del workspace", () -> this.maximized.set(false));
        this.cancel = ActionButtonFactory.danger("Cancelar", this::cancelCurrentProcess);
        this.pausePreparation = ActionButtonFactory.secondary(
                "Pausar preparación", viewModel::pauseDocumentPreparation);
        this.resumePreparation = ActionButtonFactory.secondary(
                "Reanudar preparación", viewModel::resumeDocumentPreparation);
        this.cancelPendingPreparation = ActionButtonFactory.secondary(
                "Cancelar páginas pendientes",
                cancelPendingPreparationAction == null
                        ? viewModel::cancelPendingDocumentPreparation
                        : cancelPendingPreparationAction);
        getStyleClass().add("process-overlay");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        setMinWidth(420);
        setPickOnBounds(false);

        title.getStyleClass().add("process-overlay-title");
        detail.getStyleClass().add("process-overlay-detail");
        eta.getStyleClass().add("process-overlay-eta");
        detail.setWrapText(true);
        eta.setWrapText(true);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("process-overlay-progress");
        progressPercent.getStyleClass().add("process-overlay-percent");
        progressPercent.setMinWidth(54);
        progressPercent.setAccessibleText("Porcentaje de la operación principal");
        preparationTitle.getStyleClass().add("process-overlay-preparation-title");
        preparationStatus.getStyleClass().add("process-overlay-preparation-status");
        preparationStatus.setWrapText(true);
        preparationStatus.setAccessibleText(
                "Estado de preparación progresiva del documento");
        preparationProgress.setMaxWidth(Double.MAX_VALUE);
        preparationProgress.getStyleClass().add("process-overlay-preparation-progress");
        preparationPercent.getStyleClass().add("process-overlay-percent");
        preparationPercent.setMinWidth(54);
        preparationProgress.setAccessibleText(
                "Progreso de preparación del documento");
        pausePreparation.setAccessibleText(
                "Pausar la preparación de nuevas páginas del documento");
        resumePreparation.setAccessibleText(
                "Reanudar la preparación de páginas del documento");
        cancelPendingPreparation.setAccessibleText(
                "Cancelar las páginas pendientes sin interrumpir la página actual");
        HBox preparationActions = new HBox(
                8, pausePreparation, resumePreparation, cancelPendingPreparation);
        preparationActions.setAlignment(Pos.CENTER_LEFT);
        preparationActions.getStyleClass().add(
                "process-overlay-preparation-actions");
        preparationCard.getStyleClass().add("process-overlay-preparation");
        HBox preparationProgressRow = new HBox(10, preparationProgress, preparationPercent);
        HBox.setHgrow(preparationProgress, Priority.ALWAYS);
        preparationProgressRow.setAlignment(Pos.CENTER_LEFT);
        preparationCard.getChildren().addAll(
                preparationTitle, preparationStatus,
                preparationProgressRow, preparationActions);

        hide.getStyleClass().add("process-overlay-hide-button");
        maximize.getStyleClass().add("process-overlay-size-button");
        restoreHalf.getStyleClass().add("process-overlay-size-button");
        maximize.visibleProperty().bind(this.maximized.not());
        maximize.managedProperty().bind(maximize.visibleProperty());
        restoreHalf.visibleProperty().bind(this.maximized);
        restoreHalf.managedProperty().bind(restoreHalf.visibleProperty());
        HBox head = new HBox(8, title, spacer(), maximize, restoreHalf, hide, cancel);
        head.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        HBox progressRow = new HBox(10, progress, progressPercent);
        HBox.setHgrow(progress, Priority.ALWAYS);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(
                head, detail, progressRow, eta, preparationCard);

        viewModel.activeAudioJobStatusProperty().addListener((obs, oldValue, newValue) -> {
            if ((oldValue == null || !oldValue.running()) && newValue != null && newValue.running()) {
                this.maximized.set(false);
                expanded.set(true);
            }
            render();
        });
        viewModel.audioJobRunningProperty().addListener((obs, oldValue, newValue) -> {
            if (!oldValue && newValue) {
                this.maximized.set(false);
                expanded.set(true);
            }
            render();
        });
        viewModel.pdfPreparationProgressProperty().addListener(
                (obs, oldValue, newValue) -> {
                    if (!preparationActive(oldValue)
                            && preparationActive(newValue)) {
                        this.maximized.set(false);
                        expanded.set(true);
                    }
                    render();
                });
        viewModel.localDocumentAnalysisRunningProperty().addListener(
                (obs, oldValue, newValue) -> {
                    if (!oldValue && newValue) {
                        this.maximized.set(false);
                        expanded.set(true);
                    }
                    render();
                });
        viewModel.localDocumentAnalysisTitleProperty().addListener(
                (obs, oldValue, newValue) -> render());
        viewModel.localDocumentAnalysisDetailProperty().addListener(
                (obs, oldValue, newValue) -> render());
        viewModel.localDocumentAnalysisProgressProperty().addListener(
                (obs, oldValue, newValue) -> render());
        viewModel.localDocumentAnalysisFooterProperty().addListener(
                (obs, oldValue, newValue) -> render());
        expanded.addListener((obs, oldValue, newValue) -> render());
        render();
    }

    private void render() {
        AudioJobStatusDto status = viewModel.activeAudioJobStatusProperty().get();
        AudioJobStatusDto dto = status == null ? AudioJobStatusDto.idle() : status;
        PdfPreparationProgress pdf = viewModel.pdfPreparationProgressProperty().get();
        boolean audioJobReportedRunning = dto.running();
        boolean audioRunning = audioJobReportedRunning
                || viewModel.audioJobRunningProperty().get();
        boolean localAnalysisRunning =
                viewModel.localDocumentAnalysisRunningProperty().get();
        boolean documentPreparationActive = preparationActive(pdf);
        boolean running = audioRunning || localAnalysisRunning
                || documentPreparationActive;
        boolean visible = running && expanded.get();
        setVisible(visible);
        setManaged(visible);
        hide.setDisable(!running);
        boolean cancellable = audioRunning || localAnalysisRunning;
        cancel.setVisible(cancellable);
        cancel.setManaged(cancellable);
        cancel.setDisable(!cancellable);
        boolean exportWorkflow = viewModel.documentExportWorkflowActive();
        cancel.setText(localAnalysisRunning
                ? exportWorkflow ? "Cancelar exportación" : "Detener análisis"
                : "Cancelar voz");
        cancel.setAccessibleText(localAnalysisRunning
                ? exportWorkflow
                ? "Cancelar la exportación y conservar traducciones y audios válidos"
                : "Detener el análisis local y conservar las descripciones ya guardadas"
                : "Cancelar la generación de voz");

        boolean mainActivity = audioRunning || localAnalysisRunning;
        detail.setVisible(mainActivity);
        detail.setManaged(mainActivity);
        progress.setVisible(mainActivity);
        progress.setManaged(mainActivity);
        eta.setVisible(mainActivity);
        eta.setManaged(mainActivity);
        progressPercent.setVisible(mainActivity);
        progressPercent.setManaged(mainActivity);

        if (localAnalysisRunning) {
            double analysisProgress =
                    viewModel.localDocumentAnalysisProgressProperty().get();
            title.setText(safeText(
                    viewModel.localDocumentAnalysisTitleProperty().get(),
                    "Analizando contenido del documento"));
            detail.setText(safeText(
                    viewModel.localDocumentAnalysisDetailProperty().get(),
                    "La IA local está preparando el contenido narrable."));
            progress.setProgress(analysisProgress < 0
                    ? ProgressIndicator.INDETERMINATE_PROGRESS
                    : Math.max(0.0, Math.min(1.0, analysisProgress)));
            progressPercent.setText(percentText(analysisProgress));
            if (exportWorkflow && audioJobReportedRunning) {
                eta.setText(etaText(dto, cachedSizeLabel)
                        + " La estimación corresponde al audio, no a toda la exportación.");
                requestSizeProbe(dto);
            } else eta.setText(safeText(
                    viewModel.localDocumentAnalysisFooterProperty().get(),
                    audioRunning
                            ? "La generación de voz continuará según el turno de cómputo disponible."
                            : "La operación continúa en segundo plano."));
        } else if (audioRunning) {
            if (audioJobReportedRunning) {
                title.setText(AudioProgressText.title(dto));
                detail.setText(dto.statusLine());
                progress.setProgress(dto.totalSegments() <= 0
                        ? ProgressIndicator.INDETERMINATE_PROGRESS
                        : dto.progress());
                progressPercent.setText(percentText(dto.totalSegments() <= 0
                        ? -1.0 : dto.progress()));
                eta.setText(etaText(dto, cachedSizeLabel));
                requestSizeProbe(dto);
            } else {
                title.setText("Preparando el audio");
                detail.setText(safeText(viewModel.statusMessageProperty().get(),
                        "Esperando que termine la operación de audio actual."));
                progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                progressPercent.setText("Calculando…");
                eta.setText("Puedes ocultar este panel y volver a abrirlo desde Detalles.");
            }
        } else {
            title.setText(documentPreparationActive
                    ? "Preparando el documento" : "Sin proceso activo");
        }

        renderPreparation(pdf, documentPreparationActive);
    }

    private void cancelCurrentProcess() {
        if (viewModel.localDocumentAnalysisRunningProperty().get()) {
            if (viewModel.documentExportWorkflowActive()) {
                viewModel.cancelCurrentAudioOperation();
            } else {
                cancelLocalAnalysis.run();
            }
        } else {
            viewModel.cancelCurrentAudioOperation();
        }
    }

    private void renderPreparation(PdfPreparationProgress pdf,
                                   boolean active) {
        preparationCard.setVisible(active);
        preparationCard.setManaged(active);
        if (!active) return;
        PdfPreparationProgress safe = pdf == null
                ? new PdfPreparationProgress(
                PdfPreparationProgress.State.IDLE, 0, 0, 0, 0, "")
                : pdf;
        preparationStatus.setText(preparationStatus(safe));
        preparationProgress.setProgress(safe.requested() <= 0
                ? ProgressIndicator.INDETERMINATE_PROGRESS
                : safe.fraction());
        preparationPercent.setText(safe.requested() <= 0
                ? "Calculando…" : percentText(safe.fraction()));
        boolean paused = safe.state()
                == PdfPreparationProgress.State.PAUSED;
        pausePreparation.setDisable(paused);
        resumePreparation.setDisable(!paused);
        cancelPendingPreparation.setDisable(safe.queued() == 0);
    }

    private static boolean preparationActive(PdfPreparationProgress progress) {
        return progress != null
                && (progress.state() == PdfPreparationProgress.State.RUNNING
                || progress.state() == PdfPreparationProgress.State.PAUSED
                || progress.queued() > 0);
    }

    private static String preparationStatus(PdfPreparationProgress progress) {
        if (!progress.message().isBlank()) return progress.message();
        return switch (progress.state()) {
            case RUNNING -> "Preparando la página "
                    + progress.currentPage() + ".";
            case PAUSED -> "Preparación pausada. La página actual terminará "
                    + "en un punto seguro.";
            case CANCELLED -> "Preparación cancelada.";
            case FAILED -> "La preparación encontró un problema.";
            case IDLE -> "Las páginas solicitadas están preparadas.";
        };
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String percentText(double value) {
        if (!Double.isFinite(value) || value < 0.0) return "Calculando…";
        return Math.round(Math.max(0.0, Math.min(1.0, value)) * 100.0) + " %";
    }

    private String etaText(AudioJobStatusDto dto, String sizeLabel) {
        return AudioProgressText.timing(dto) + " · " + dto.completedSegments() + "/"
                + dto.totalSegments() + " fragmentos de este lote · " + sizeLabel
                + ". Puedes ocultar este panel; la generación continuará y podrás reabrir los detalles desde la barra de estado.";
    }

    private void requestSizeProbe(AudioJobStatusDto dto) {
        if (dto == null || !dto.running() || dto.outputDirectory().isBlank() || dto.totalSegments() <= 0) {
            return;
        }
        long now = System.nanoTime();
        String key = dto.jobId() + ":" + dto.completedSegments() + ":" + dto.totalSegments() + ":" + dto.outputDirectory();
        if (Objects.equals(key, sizeProbeKey) || now - lastSizeProbeNanos < SIZE_PROBE_INTERVAL_NANOS || !sizeProbeRunning.compareAndSet(false, true)) {
            return;
        }
        sizeProbeKey = key;
        lastSizeProbeNanos = now;
        SIZE_PROBE_EXECUTOR.submit(() -> {
            String label = generatedChunksSizeLabel(dto);
            Platform.runLater(() -> {
                cachedSizeLabel = label;
                AudioJobStatusDto current = viewModel.activeAudioJobStatusProperty().get();
                if (current != null && dto.jobId().equals(current.jobId())) {
                    render();
                }
                sizeProbeRunning.set(false);
            });
        });
    }

    private static String generatedChunksSizeLabel(AudioJobStatusDto dto) {
        long completedBytes = completedWavBytes(dto.outputDirectory());
        if (completedBytes <= 0L) {
            return dto.completedSegments() <= 0
                    ? "audio generado en disco: 0 B"
                    : "audio generado en disco: calculando";
        }
        return "audio generado en disco: " + humanSize(completedBytes);
    }

    private static long completedWavBytes(String outputDirectory) {
        try (Stream<Path> files = Files.walk(Path.of(outputDirectory))) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav"))
                    .mapToLong(LongProcessOverlayView::safeSize)
                    .sum();
        } catch (IllegalArgumentException | IOException | SecurityException ex) {
            return 0L;
        }
    }

    private static long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException | SecurityException ex) {
            return 0L;
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }

    private static javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
