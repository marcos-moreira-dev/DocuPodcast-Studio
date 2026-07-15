package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.stage.Window;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Gate kept for shell commands that used to prepare PDF text automatically.
 *
 * <p>PDF OCR is intentionally click-driven in the visual document view. Reading and
 * audio commands must consume only OCR blocks that already exist.</p>
 */
public final class PdfNarratablePreparationCoordinator {
    public PdfNarratablePreparationCoordinator(DocuPodcastShellViewModel viewModel,
                                               FxBackgroundTaskRunner backgroundTaskRunner,
                                               ExceptionAlertPresenter alertPresenter,
                                               Supplier<Window> ownerSupplier) {
        Objects.requireNonNull(viewModel, "viewModel");
        Objects.requireNonNull(backgroundTaskRunner, "backgroundTaskRunner");
        Objects.requireNonNull(alertPresenter, "alertPresenter");
        Objects.requireNonNull(ownerSupplier, "ownerSupplier");
    }

    public void prepareThenRun(Runnable continuation) {
        runContinuation(continuation);
    }

    public void prepareForwardThenRun(Runnable continuation) {
        runContinuation(continuation);
    }

    private static void runContinuation(Runnable continuation) {
        if (continuation != null) {
            continuation.run();
        }
    }
}
