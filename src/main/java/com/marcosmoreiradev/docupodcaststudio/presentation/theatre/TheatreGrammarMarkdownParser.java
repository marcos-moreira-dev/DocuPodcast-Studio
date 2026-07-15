package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;

import java.io.IOException;
import java.nio.file.Path;

/** Compatibility facade for the application-level Theatre Grammar v1 parser. */
public final class TheatreGrammarMarkdownParser {
    private TheatreGrammarMarkdownParser() {
    }

    public static ImportPlan parse(Path sourceFile) throws IOException {
        return com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarMarkdownParser.parse(sourceFile);
    }

    public static ImportPlan parse(String markdown) {
        return com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarMarkdownParser.parse(markdown);
    }
}
