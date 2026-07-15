package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Round-trip request for a user-level DocuPodcast project.
 *
 * <p>The document source remains read-only. The request only materializes project-side
 * artifacts that DocuPodcast owns: normalized document snapshot, internal narration
 * projection, narrative layers in the root project, storyboard manifest and persisted
 * audio job snapshots.</p>
 */
public record ProjectRoundTripRequest(
        DocuPodcastProject project,
        Path projectFile,
        ReadableDocument importedDocument,
        NarrationScriptDocument narrationProjection,
        StoryboardDocument storyboard,
        List<AudioJobSnapshot> audioJobs
) {
    public ProjectRoundTripRequest {
        project = Objects.requireNonNull(project, "project");
        projectFile = Objects.requireNonNull(projectFile, "projectFile");
        audioJobs = audioJobs == null ? List.of() : List.copyOf(audioJobs);
    }

    public static ProjectRoundTripRequest of(
            DocuPodcastProject project,
            Path projectFile,
            ReadableDocument importedDocument,
            NarrationScriptDocument narrationProjection,
            StoryboardDocument storyboard,
            List<AudioJobSnapshot> audioJobs
    ) {
        return new ProjectRoundTripRequest(project, projectFile, importedDocument, narrationProjection, storyboard, audioJobs);
    }

    public Optional<ReadableDocument> importedDocumentOptional() {
        return Optional.ofNullable(importedDocument);
    }

    public Optional<NarrationScriptDocument> narrationProjectionOptional() {
        return Optional.ofNullable(narrationProjection);
    }

    public Optional<StoryboardDocument> storyboardOptional() {
        return Optional.ofNullable(storyboard);
    }
}
