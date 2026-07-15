package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Persists the live-storyboard manifest next to the project file. */
public interface StoryboardWorkspaceRepository {
    MaterializedStoryboard materialize(Path projectFile, StoryboardDocument storyboard) throws IOException;

    Optional<StoryboardDocument> load(Path projectFile) throws IOException;
}
