package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;

import java.util.List;
import java.util.Objects;

/** Minimal presentation state needed to explain export readiness without owning export logic. */
public record ExportCenterState(
        ProjectMode mode,
        boolean projectOpen,
        ExportReadinessReport readinessReport,
        List<ProcessJobSnapshot> processJobs
) {
    public ExportCenterState {
        mode = Objects.requireNonNullElse(mode, ProjectMode.defaultMode());
        processJobs = processJobs == null ? List.of() : List.copyOf(processJobs);
    }
}
