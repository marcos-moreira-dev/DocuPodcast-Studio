package com.marcosmoreiradev.docupodcaststudio.application.project;

import java.util.List;

/** Passive post-open report; it never starts regeneration. */
public record ProjectDerivedAssetReport(List<String> warnings) {
    public ProjectDerivedAssetReport {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
    public boolean degraded() { return !warnings.isEmpty(); }
    public static ProjectDerivedAssetReport empty() { return new ProjectDerivedAssetReport(List.of()); }
}
