package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import java.io.IOException;
import java.nio.file.Path;

/** Persists the project-level machine-readable grammar semantics sidecar. */
public interface ProjectSemanticsRepository {
    void save(Path projectDirectory, ProjectSemanticsDocument document) throws IOException;
}
