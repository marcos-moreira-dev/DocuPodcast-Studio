package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.nio.file.Path;
import java.util.List;

/** Category extension point for global visual assets outside document fragments. */
@FunctionalInterface
public interface VisualGlobalAssetProjectionProvider {
    List<VisualSlotState> globalVisuals(DocuPodcastProject project, Path projectDirectory);

    static VisualGlobalAssetProjectionProvider none() {
        return (project, projectDirectory) -> List.of();
    }
}
