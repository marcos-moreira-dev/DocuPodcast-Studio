package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Local state discovered before any managed network request is attempted. */
public enum ManagedDownloadState {
    MISSING,
    VALID,
    INVALID,
    PARTIAL
}
