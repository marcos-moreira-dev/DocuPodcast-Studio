package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVideoGrammarTemplate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarTemplate;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

/** Selects the official downloadable Markdown template for a supported project grammar. */
public final class BuildGrammarTemplateUseCase {
    public Template templateFor(ProjectGrammarKind kind) {
        ProjectGrammarKind resolved = java.util.Objects.requireNonNull(kind, "kind");
        return switch (resolved) {
            case NARRATIVE_VIDEO -> new Template(resolved, resolved.defaultFileName(), NarrativeVideoGrammarTemplate.markdown());
            case THEATRE_PRODUCTION -> new Template(resolved, resolved.defaultFileName(), TheatreGrammarTemplate.markdown());
        };
    }

    public Template templateFor(ProjectMode mode) {
        ProjectGrammarKind kind = ProjectGrammarKind.forMode(mode)
                .orElseThrow(() -> new IllegalArgumentException("El modo Estudio documental no requiere plantilla Markdown."));
        return templateFor(kind);
    }

    public record Template(ProjectGrammarKind kind, String defaultFileName, String markdown) {
    }
}
