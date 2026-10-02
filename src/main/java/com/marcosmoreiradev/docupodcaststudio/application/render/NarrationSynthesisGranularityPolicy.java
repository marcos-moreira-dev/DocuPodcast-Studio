package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.util.Objects;

/** Chooses the physical audio unit without changing the semantic script model. */
public final class NarrationSynthesisGranularityPolicy {
    public enum Granularity {
        SENTENCE,
        SEMANTIC_SEGMENT
    }

    private final ProjectModePolicy projectModePolicy;

    public NarrationSynthesisGranularityPolicy() {
        this(new ProjectModePolicy());
    }

    NarrationSynthesisGranularityPolicy(ProjectModePolicy projectModePolicy) {
        this.projectModePolicy = Objects.requireNonNull(projectModePolicy, "projectModePolicy");
    }

    public Granularity resolve(DocuPodcastProject project) {
        return projectModePolicy.resolve(project) == ProjectMode.THEATRE_PRODUCTION
                ? Granularity.SEMANTIC_SEGMENT
                : Granularity.SENTENCE;
    }
}
