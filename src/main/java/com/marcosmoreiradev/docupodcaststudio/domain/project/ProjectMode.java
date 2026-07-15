package com.marcosmoreiradev.docupodcaststudio.domain.project;

import java.util.List;

/** Official user-facing project mode. ProjectKind remains the technical lifecycle state. */
public enum ProjectMode {
    DOCUMENTARY_STUDIO("Estudio documental"),
    NARRATIVE_VIDEO("Video narrativo"),
    THEATRE_PRODUCTION("Producción teatral");

    private final String displayName;

    ProjectMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static ProjectMode defaultMode() {
        return DOCUMENTARY_STUDIO;
    }

    public static List<ProjectMode> officialModes() {
        return List.of(DOCUMENTARY_STUDIO, NARRATIVE_VIDEO, THEATRE_PRODUCTION);
    }
}
