package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.nio.file.Path;

/** Result of writing storyboard/storyboard.json. */
public record MaterializedStoryboard(Path absolutePath, ProjectAssetReference asset) {
}
