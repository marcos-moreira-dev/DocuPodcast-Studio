package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Neutral workload families used for scheduling, diagnostics and UI. */
public enum ComputeWorkloadKind {
    VOICE_SYNTHESIS,
    CONTENT_ANALYSIS,
    DOCUMENT_PREPARATION,
    OCR,
    MATH_SPEECH,
    IMAGE_PROCESSING,
    VIDEO_RENDER,
    OTHER
}
