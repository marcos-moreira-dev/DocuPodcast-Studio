package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.Objects;

/** Resolves the official project mode while preserving legacy ProjectKind semantics. */
public final class ProjectModePolicy {
    public ProjectMode resolve(DocuPodcastProject project) {
        if (project == null || project.metadata() == null) {
            return ProjectMode.defaultMode();
        }
        if (hasTheatreData(project.theatre())) {
            return ProjectMode.THEATRE_PRODUCTION;
        }
        return Objects.requireNonNullElse(project.metadata().mode(), inferLegacy(project.metadata().kind(), project.theatre()));
    }

    public ProjectMode inferLegacy(ProjectKind kind, TheatreProjectLayer theatre) {
        if (hasTheatreData(theatre)) {
            return ProjectMode.THEATRE_PRODUCTION;
        }
        ProjectKind resolved = Objects.requireNonNullElse(kind, ProjectKind.EMPTY);
        return switch (resolved) {
            case STORYBOARD, FULL_PROJECT -> ProjectMode.NARRATIVE_VIDEO;
            default -> ProjectMode.DOCUMENTARY_STUDIO;
        };
    }

    public boolean hasTheatreData(TheatreProjectLayer theatre) {
        return theatre != null
                && (!theatre.intervenciones().isEmpty()
                || !theatre.characters().isEmpty()
                || !theatre.voiceRoleAliases().isEmpty()
                || !theatre.characterImages().isEmpty()
                || !theatre.intervencionesVisuales().isEmpty()
                || !theatre.acts().isEmpty()
                || !theatre.scenes().isEmpty()
                || !theatre.positions().isEmpty()
                || !theatre.actions().isEmpty()
                || !theatre.textActionPlacements().isEmpty()
                || !theatre.objectImages().isEmpty()
                || !theatre.objects().isEmpty());
    }
}
