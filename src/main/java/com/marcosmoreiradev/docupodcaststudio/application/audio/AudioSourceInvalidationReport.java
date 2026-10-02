package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.List;

/** Granular reuse decision for persisted audio units. */
public record AudioSourceInvalidationReport(
        List<String> reusableSegmentIds,
        List<String> staleSegmentIds,
        List<String> missingSegmentIds
) {
    public AudioSourceInvalidationReport {
        reusableSegmentIds = reusableSegmentIds == null ? List.of() : List.copyOf(reusableSegmentIds);
        staleSegmentIds = staleSegmentIds == null ? List.of() : List.copyOf(staleSegmentIds);
        missingSegmentIds = missingSegmentIds == null ? List.of() : List.copyOf(missingSegmentIds);
    }
}
