package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.util.Optional;

/** Official Markdown grammar contracts exposed by project mode. */
public enum ProjectGrammarKind {
    NARRATIVE_VIDEO("Video narrativo", "narrative-video-v1", "plantilla-gramatica-video-narrativo.md"),
    THEATRE_PRODUCTION("Produccion teatral", "theatre-v2", "plantilla-gramatica-teatral.md");

    private final String displayName;
    private final String grammarVersion;
    private final String defaultFileName;

    ProjectGrammarKind(String displayName, String grammarVersion, String defaultFileName) {
        this.displayName = displayName;
        this.grammarVersion = grammarVersion;
        this.defaultFileName = defaultFileName;
    }

    public String displayName() {
        return displayName;
    }

    public String grammarVersion() {
        return grammarVersion;
    }

    public String defaultFileName() {
        return defaultFileName;
    }

    public static Optional<ProjectGrammarKind> forMode(ProjectMode mode) {
        if (mode == null) {
            return Optional.empty();
        }
        return switch (mode) {
            case NARRATIVE_VIDEO -> Optional.of(NARRATIVE_VIDEO);
            case THEATRE_PRODUCTION -> Optional.of(THEATRE_PRODUCTION);
            case DOCUMENTARY_STUDIO -> Optional.empty();
        };
    }
}
