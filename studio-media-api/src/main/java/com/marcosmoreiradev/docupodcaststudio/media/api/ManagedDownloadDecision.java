package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Explicit user decision for a managed download that may already exist locally. */
public enum ManagedDownloadDecision {
    USE_EXISTING,
    REDOWNLOAD,
    CANCEL
}
