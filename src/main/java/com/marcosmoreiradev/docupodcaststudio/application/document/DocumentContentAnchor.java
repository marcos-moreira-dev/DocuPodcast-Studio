package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Stable source anchor without leaking a DOCX- or PDF-specific model into panels. */
public sealed interface DocumentContentAnchor permits WordContentAnchor, PdfContentAnchor {
}
