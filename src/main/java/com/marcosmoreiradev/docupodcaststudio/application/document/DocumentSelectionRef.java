package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Stable selection identity across block documents and canonical PDF V2 workspaces. */
public sealed interface DocumentSelectionRef permits BlockSelectionRef, PdfRegionSelectionRef {
}
