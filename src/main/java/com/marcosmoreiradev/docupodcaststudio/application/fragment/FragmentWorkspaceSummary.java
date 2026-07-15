package com.marcosmoreiradev.docupodcaststudio.application.fragment;

/** Small mode-agnostic readiness summary derived from FragmentWorkspaceProjection. */
public record FragmentWorkspaceSummary(
        int fragmentCount,
        int audioBindingCount,
        int visualBindingCount,
        int theatreBindingCount,
        int playbackCueCount
) {
}
