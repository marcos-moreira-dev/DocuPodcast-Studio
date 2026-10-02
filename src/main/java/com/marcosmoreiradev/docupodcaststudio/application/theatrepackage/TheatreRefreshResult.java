package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectIntegrityReport;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.Objects;

public record TheatreRefreshResult(
        Status status,
        DocuPodcastProject project,
        TheatreRefreshPreflightReport preflight,
        ProjectIntegrityReport integrity,
        String message
) {
    public enum Status { NO_CHANGES, APPLIED }

    public TheatreRefreshResult {
        status = Objects.requireNonNull(status, "status");
        project = Objects.requireNonNull(project, "project");
        preflight = Objects.requireNonNull(preflight, "preflight");
        message = Objects.requireNonNullElse(message, "").strip();
    }
}
