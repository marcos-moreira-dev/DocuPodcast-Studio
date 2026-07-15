package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.Optional;

/** Materialized workspace artifacts recovered when opening a .docupodcast project folder. */
public record ProjectWorkspaceHydration(
        Optional<ReadableDocument> importedDocument,
        Optional<NarrationScriptDocument> narrationScript,
        Optional<StoryboardDocument> storyboard
) {
    public ProjectWorkspaceHydration {
        importedDocument = importedDocument == null ? Optional.empty() : importedDocument;
        narrationScript = narrationScript == null ? Optional.empty() : narrationScript;
        storyboard = storyboard == null ? Optional.empty() : storyboard;
    }

    public static ProjectWorkspaceHydration empty() {
        return new ProjectWorkspaceHydration(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public int loadedArtifactCount() {
        int count = 0;
        if (importedDocument.isPresent()) count++;
        if (narrationScript.isPresent()) count++;
        if (storyboard.isPresent()) count++;
        return count;
    }

    public String statusLabel() {
        if (loadedArtifactCount() == 0) {
            return "sin artefactos materializados";
        }
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        importedDocument.ifPresent(document -> parts.add("documento " + document.blocks().size() + " bloques"));
        narrationScript.ifPresent(script -> parts.add("lectura preparada " + script.segmentCount() + " segmentos"));
        storyboard.ifPresent(value -> parts.add("storyboard " + value.bindingCount() + " imágenes"));
        return String.join(", ", parts);
    }
}
