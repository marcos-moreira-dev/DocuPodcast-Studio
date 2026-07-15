package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** Validation summary for engine artifact manifests before a final release. */
public record EngineArtifactValidationReport(boolean readyForFinalRc, List<String> issues) {
    public EngineArtifactValidationReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
