package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.Optional;

/** Materialized workspace artifacts recovered when opening a .docupodcast project folder. */
public record ProjectWorkspaceHydration(
        Optional<ProjectDocumentSource> documentSource,
        Optional<NarrationScriptDocument> narrationScript,
        Optional<StoryboardDocument> storyboard
) {
    public ProjectWorkspaceHydration {
        documentSource = documentSource == null ? Optional.empty() : documentSource;
        narrationScript = narrationScript == null ? Optional.empty() : narrationScript;
        storyboard = storyboard == null ? Optional.empty() : storyboard;
    }

    public static ProjectWorkspaceHydration empty() {
        return new ProjectWorkspaceHydration(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public int loadedArtifactCount() {
        int count = 0;
        if (documentSource.isPresent()) count++;
        if (narrationScript.isPresent()) count++;
        if (storyboard.isPresent()) count++;
        return count;
    }

    public String statusLabel() {
        if (loadedArtifactCount() == 0) {
            return "sin artefactos materializados";
        }
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        documentSource.ifPresent(source -> parts.add(source instanceof PreparedPdfSource
                ? "PDF V2 preparado"
                : "documento " + ((BlockDocumentSource) source).document().blocks().size() + " bloques"));
        narrationScript.ifPresent(script -> parts.add("lectura preparada " + script.segmentCount() + " segmentos"));
        storyboard.ifPresent(value -> parts.add("storyboard " + value.bindingCount() + " imágenes"));
        return String.join(", ", parts);
    }

    public Optional<ReadableDocument> importedDocument() {
        return documentSource.filter(BlockDocumentSource.class::isInstance)
                .map(BlockDocumentSource.class::cast)
                .map(BlockDocumentSource::document);
    }

    public Optional<PreparedPdfSource> preparedPdfSource() {
        return documentSource.filter(PreparedPdfSource.class::isInstance)
                .map(PreparedPdfSource.class::cast);
    }
}
