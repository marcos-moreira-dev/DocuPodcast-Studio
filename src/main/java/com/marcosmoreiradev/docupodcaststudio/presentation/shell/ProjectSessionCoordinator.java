package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Coordinates the single active project session used by the early shell. */
public final class ProjectSessionCoordinator {
    private ProjectSession activeSession;

    public ProjectSession startNew(DocuPodcastProject project) {
        activeSession = ProjectSession.newUnsaved(project);
        return activeSession;
    }

    public ProjectSession open(DocuPodcastProject project, Path projectFile) {
        activeSession = ProjectSession.opened(project, Objects.requireNonNull(projectFile, "projectFile"));
        return activeSession;
    }

    public Optional<ProjectSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    public boolean hasActiveSession() {
        return activeSession != null;
    }

    public boolean dirty() {
        return activeSession != null && activeSession.dirty();
    }

    public boolean saveable() {
        return activeSession != null && activeSession.saveable();
    }

    public void closeActive() {
        activeSession = null;
    }
}
