package com.marcosmoreiradev.docupodcaststudio.application.artifacts;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Project-scoped artifact access used by application and presentation coordinators.
 * Implementations must confine every resolved or discarded path to the project root.
 */
public interface ProjectArtifactStore {
    Path projectRoot(Path projectFile) throws IOException;

    Path resolveExisting(Path projectFile, String relativePath) throws IOException;

    boolean isProjectOwnedRegularFile(Path projectFile, Path candidate) throws IOException;

    void discardProjectOwned(Path projectFile, Path candidate) throws IOException;
}
