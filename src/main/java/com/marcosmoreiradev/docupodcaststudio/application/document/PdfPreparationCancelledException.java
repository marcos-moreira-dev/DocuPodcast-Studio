package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Internal control-flow exception for cooperative PDF preparation cancellation. */
public final class PdfPreparationCancelledException extends RuntimeException {
    public PdfPreparationCancelledException() {
        super("La preparación PDF fue cancelada.");
    }
}
