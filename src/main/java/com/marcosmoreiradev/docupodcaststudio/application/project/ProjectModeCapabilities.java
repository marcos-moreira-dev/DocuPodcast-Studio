package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.util.Objects;

/** Capability profile for the official user-facing project modes. */
public record ProjectModeCapabilities(
        ProjectMode mode,
        boolean documentStudy,
        boolean audioProduction,
        boolean narrativeVisuals,
        boolean theatreProduction
) {
    public ProjectModeCapabilities {
        mode = Objects.requireNonNullElse(mode, ProjectMode.defaultMode());
    }

    public static ProjectModeCapabilities forMode(ProjectMode mode) {
        ProjectMode resolved = Objects.requireNonNullElse(mode, ProjectMode.defaultMode());
        return switch (resolved) {
            case DOCUMENTARY_STUDIO -> new ProjectModeCapabilities(resolved, true, true, false, false);
            case NARRATIVE_VIDEO -> new ProjectModeCapabilities(resolved, true, true, true, false);
            case THEATRE_PRODUCTION -> new ProjectModeCapabilities(resolved, true, true, true, true);
        };
    }
}
