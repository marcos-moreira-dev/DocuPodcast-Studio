package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Cooperative cancellation checked at safe PDF preparation boundaries. */
@FunctionalInterface
public interface PdfPreparationCancellationToken {
    PdfPreparationCancellationToken NONE = () -> false;

    boolean cancelled();

    default void throwIfCancelled() {
        if (cancelled()) {
            throw new PdfPreparationCancelledException();
        }
    }
}
