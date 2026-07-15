package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Path;
import java.util.Objects;

/** Completed, project-ready result of a theatre choral voice render. */
public record TheatreChoralVoiceRenderResult(
        DocuPodcastProject project,
        TheatreProjectLayer.ChoralVoiceAssignment assignment,
        Path outputFile,
        String message
) {
    public TheatreChoralVoiceRenderResult {
        project = Objects.requireNonNull(project, "project");
        assignment = Objects.requireNonNull(assignment, "assignment");
        outputFile = Objects.requireNonNull(outputFile, "outputFile").toAbsolutePath().normalize();
        message = message == null ? "" : message.strip();
    }
}
