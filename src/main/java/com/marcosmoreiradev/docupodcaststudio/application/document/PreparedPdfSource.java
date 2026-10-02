package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.nio.file.Path;
import java.util.Objects;

/** Canonical PDF source backed exclusively by manifest.json and page JSON files. */
public record PreparedPdfSource(
        PreparedPdfWorkspaceRef workspace,
        String title
) implements ProjectDocumentSource {
    public PreparedPdfSource {
        workspace = Objects.requireNonNull(workspace, "workspace");
        title = title == null || title.isBlank() ? "Documento PDF" : title.strip();
    }

    @Override public SourceDocumentFormat format() { return SourceDocumentFormat.PDF; }
    @Override public Path sourcePath() { return workspace.sourcePath(); }
}
