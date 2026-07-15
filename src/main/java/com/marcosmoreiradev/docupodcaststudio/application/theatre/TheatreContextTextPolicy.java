package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.Optional;

/** Stores per-fragment editable AI context text in project view state. */
public final class TheatreContextTextPolicy {
    private static final String PREFIX = "theatre.contextText.";

    public Optional<String> text(DocuPodcastProject project, String segmentId) {
        if (project == null || segmentId == null || segmentId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(project.viewState().get(key(segmentId)))
                .filter(value -> !value.isBlank());
    }

    public DocuPodcastProject save(DocuPodcastProject project, String segmentId, String text) {
        if (project == null || segmentId == null || segmentId.isBlank()) {
            return project;
        }
        String normalized = text == null ? "" : text.strip();
        return project.withViewState(key(segmentId), normalized.isBlank() ? null : normalized);
    }

    private static String key(String segmentId) {
        return PREFIX + segmentId.strip();
    }
}
