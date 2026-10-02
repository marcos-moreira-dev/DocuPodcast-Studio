package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;

/** Selection-independent presentation state of an original Word/PDF visual. */
public record DocumentarySourceVisualPresentation(State state, String label) {
    public enum State {
        NO_VISUAL,
        VISUAL_AVAILABLE_NOT_MATERIALIZED,
        VISUAL_LOADING,
        VISUAL_READY,
        VISUAL_FAILED
    }

    public static DocumentarySourceVisualPresentation of(DocumentContentItem content,
                                                          boolean materialized) {
        if (content == null
                || content.presentationMode() == DocumentPresentationMode.TEXT_RENDER) {
            return new DocumentarySourceVisualPresentation(State.NO_VISUAL, "TEXT_RENDER");
        }
        boolean pdf = content != null && content.pdfAnchor().isPresent();
        boolean word = content != null && content.wordAnchor().map(anchor -> !anchor.metadata()
                .getOrDefault("embeddedImageBase64", "").isBlank()).orElse(false);
        if (!pdf && !word) return new DocumentarySourceVisualPresentation(State.NO_VISUAL, "Sin imagen");
        String label = pdf ? "Imagen original PDF" : "Imagen original Word";
        return new DocumentarySourceVisualPresentation(materialized ? State.VISUAL_READY
                : State.VISUAL_AVAILABLE_NOT_MATERIALIZED,
                materialized ? label : label + " pendiente");
    }
}
