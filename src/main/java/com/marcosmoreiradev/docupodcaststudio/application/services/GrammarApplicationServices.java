package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.grammar.BuildGrammarTemplateUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ImportProjectGrammarMarkdownUseCase;

/** Markdown grammar templates, imports and project-semantics materialization. */
public record GrammarApplicationServices(
        BuildGrammarTemplateUseCase buildGrammarTemplate,
        ImportProjectGrammarMarkdownUseCase importProjectGrammarMarkdown
) {
}
