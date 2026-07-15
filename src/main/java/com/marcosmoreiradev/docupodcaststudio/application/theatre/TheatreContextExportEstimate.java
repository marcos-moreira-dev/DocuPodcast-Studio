package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.List;

/** Preflight estimate for exporting repeated theatre AI context packages. */
public record TheatreContextExportEstimate(
        long estimatedBytes,
        int packages,
        int files,
        int acts,
        int scenes,
        int interventions,
        List<String> warnings
) {
    public TheatreContextExportEstimate {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
