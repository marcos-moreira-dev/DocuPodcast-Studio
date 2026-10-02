package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Runs Word image interpretation only from an explicit listening/processing action. */
public final class WordSemanticPreparationCoordinator {
    private final DocuPodcastShellViewModel viewModel;
    private final FxBackgroundTaskRunner backgroundTasks;
    private final AtomicLong generation = new AtomicLong();
    private volatile Thread worker;

    public WordSemanticPreparationCoordinator(DocuPodcastShellViewModel viewModel,
                                              FxBackgroundTaskRunner backgroundTasks) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.backgroundTasks = Objects.requireNonNull(backgroundTasks, "backgroundTasks");
    }

    public void prepareThenRun(Runnable continuation) {
        prepare(continuation, null);
    }

    /** Batch variant that reports cancellation/persistence failure to the queue. */
    public void prepareForBatchThenRun(
            Runnable continuation, Consumer<Throwable> failureHandler) {
        prepare(continuation, Objects.requireNonNull(failureHandler, "failureHandler"), "");
    }

    public void prepareForBatchThenRun(
            Runnable continuation, Consumer<Throwable> failureHandler, String engineId) {
        prepare(continuation, Objects.requireNonNull(failureHandler, "failureHandler"), engineId);
    }

    private void prepare(Runnable continuation, Consumer<Throwable> failureHandler) {
        prepare(continuation, failureHandler, "");
    }

    private void prepare(Runnable continuation, Consumer<Throwable> failureHandler,
                         String engineId) {
        Objects.requireNonNull(continuation, "continuation");
        if (!viewModel.requiresWordSemanticImagePreparation()) {
            continuation.run();
            return;
        }
        viewModel.updateStatusMessage(
                "Describiendo imágenes Word autorizadas con el Modelo de IA...");
        long token = generation.incrementAndGet();
        viewModel.beginLocalDocumentAnalysis(
                "Interpretando imágenes Word",
                "El Modelo de IA está preparando los componentes autorizados.");
        worker = backgroundTasks.start("word-semantic-image-preparation", () -> {
            try {
                var result = viewModel.prepareWordSemanticImages(engineId, progress -> {
                    String eta = progress.estimatedRemainingSeconds() < 0
                            ? "calculando tiempo restante"
                            : "faltan aproximadamente "
                            + duration(progress.estimatedRemainingSeconds());
                    viewModel.updateLocalDocumentAnalysisProgress(
                            progress.completed(), progress.total(),
                            "Imágenes " + progress.completed() + "/" + progress.total()
                                    + (progress.currentBlockId().isBlank() ? ""
                                    : " · " + progress.currentBlockId()));
                    viewModel.updateLocalDocumentAnalysisFooter(
                            "Descritas " + progress.generated() + "; reutilizadas "
                                    + progress.reused() + "; omitidas " + progress.failed()
                                    + " · " + eta + ".");
                });
                Platform.runLater(() -> {
                    if (token != generation.get()) return;
                    worker = null;
                    try {
                        viewModel.applyPreparedWordSemanticImages(result);
                    } catch (java.io.IOException persistenceFailure) {
                        viewModel.endLocalDocumentAnalysis();
                        viewModel.updateStatusMessage(
                                "Las descripciones se completaron en memoria, pero no se pudieron guardar: "
                                        + rootMessage(persistenceFailure));
                        if (failureHandler != null) failureHandler.accept(persistenceFailure);
                        return;
                    }
                    if (result.cancelled()) {
                        viewModel.endLocalDocumentAnalysis();
                        viewModel.updateStatusMessage(
                                "Descripción de imágenes Word cancelada; se guardaron "
                                        + result.generated() + " descripciones nuevas y se conservaron "
                                        + result.reused() + " vigentes.");
                        if (failureHandler != null) failureHandler.accept(
                                new IllegalStateException("La preparación de imágenes Word fue cancelada."));
                        return;
                    }
                    if (result.failures().isEmpty()) {
                        viewModel.updateStatusMessage("Imagenes Word preparadas: "
                                + result.generated() + " nuevas; " + result.reused()
                                + " reutilizadas.");
                    } else {
                        viewModel.updateStatusMessage("Imagenes Word: " + result.generated()
                                + " descritas; " + result.failures().size()
                                + " omitidas por evidencia insuficiente.");
                    }
                    viewModel.endLocalDocumentAnalysis();
                    continuation.run();
                });
            } catch (InterruptedException cancelled) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    if (token != generation.get()) return;
                    worker = null;
                    viewModel.endLocalDocumentAnalysis();
                    viewModel.updateStatusMessage(
                            "Descripcion de imagenes Word cancelada; se conserva lo terminado.");
                    if (failureHandler != null) failureHandler.accept(
                            new IllegalStateException("La preparación de imágenes Word fue cancelada."));
                });
            } catch (Exception failure) {
                Platform.runLater(() -> {
                    if (token != generation.get()) return;
                    worker = null;
                    viewModel.endLocalDocumentAnalysis();
                    viewModel.updateStatusMessage("No se pudieron preparar las imagenes Word: "
                            + rootMessage(failure) + ". Se continuara con el texto disponible.");
                    continuation.run();
                });
            }
        });
    }

    public boolean cancelLocalAnalysis() {
        Thread active = worker;
        if (active == null || !active.isAlive()) return false;
        active.interrupt();
        viewModel.updateStatusMessage(
                "Cancelación solicitada; guardando las descripciones ya terminadas...");
        return true;
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName() : message;
    }

    private static String duration(long seconds) {
        long safe = Math.max(0L, seconds);
        long hours = safe / 3600;
        long minutes = (safe % 3600) / 60;
        long remainder = safe % 60;
        if (hours > 0) return hours + " h " + minutes + " min";
        if (minutes > 0) return minutes + " min " + remainder + " s";
        return remainder + " s";
    }
}
