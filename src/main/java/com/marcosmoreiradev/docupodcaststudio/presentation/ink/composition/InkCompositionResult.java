package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceState;
import javafx.scene.image.WritableImage;

import java.nio.file.Path;
import java.util.Map;

/** Flattened output plus its editable state and staged source images. */
public record InkCompositionResult(
        WritableImage image,
        InkWorkspaceState state,
        Map<String, Path> stagedSources) {

    public InkCompositionResult {
        stagedSources = stagedSources == null ? Map.of() : Map.copyOf(stagedSources);
    }
}
