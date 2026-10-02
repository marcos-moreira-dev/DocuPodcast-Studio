package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

/** Optional local interpretation that can be derived from preserved PDF regions. */
public enum PdfDerivedTreatmentKind {
    TABLE_STRUCTURE,
    TABLE_NARRATION,
    SMALL_TABLE_NARRATION,
    MATHEMATICAL_READING,
    IMAGE_DESCRIPTION,
    CONTEXTUAL_CORRECTION,
    NARRATABILITY_REVIEW,
    LIGHTWEIGHT_LANGUAGE_MODEL
}
