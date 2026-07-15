package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.Objects;

/** Removes one theatre background track without deleting its imported source asset. */
public final class RemoveTheatreAudioTrackUseCase {
    public DocuPodcastProject execute(DocuPodcastProject project, String trackId) {
        Objects.requireNonNull(project, "project");
        String target = trackId == null ? "" : trackId.strip();
        if (target.isBlank()) {
            return project;
        }
        return project.withTheatre(project.theatre().withAudioTracks(project.theatre().audioTracks().stream()
                .filter(track -> !track.id().equals(target))
                .toList()));
    }
}
