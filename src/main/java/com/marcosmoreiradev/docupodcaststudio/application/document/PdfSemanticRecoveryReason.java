package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Small strategy vocabulary derived from existing coverage diagnostics. */
public enum PdfSemanticRecoveryReason {
    DENSE_TABLE_INCOMPLETE,
    TEXT_COVERAGE_GAP,
    MULTICOLUMN_GAP_OR_ORDER,
    VISUAL_REGION_INCOMPLETE,
    PROMINENT_TEXT_UNCOVERED,
    SEGMENTATION_REFINEMENT,
    UNKNOWN
}
