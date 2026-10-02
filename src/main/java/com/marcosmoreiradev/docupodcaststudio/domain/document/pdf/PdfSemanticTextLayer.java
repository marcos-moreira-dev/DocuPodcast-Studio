package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Semantic layer that contributed text to a PDF narration segment. */
public enum PdfSemanticTextLayer {
    /** Text observed by extraction/OCR or explicitly corrected by a person. */
    LITERAL,
    /** Description, explanation or other reviewed derivative. */
    INTERPRETATION,
    /** Deterministic speech added by product policy, such as an announcement. */
    NARRATION
}
