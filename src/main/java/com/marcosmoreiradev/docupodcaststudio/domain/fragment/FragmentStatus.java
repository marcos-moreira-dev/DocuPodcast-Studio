package com.marcosmoreiradev.docupodcaststudio.domain.fragment;

/** Derived status of a document fragment in the cross-media workspace projection. */
public enum FragmentStatus {
    SOURCE_ONLY,
    NARRATABLE,
    AUDIO_PENDING,
    AUDIO_GENERATING,
    AUDIO_READY,
    AUDIO_FAILED,
    VISUAL_ONLY
}
