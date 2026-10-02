package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;

/** Per-attempt artifact staging. Promotion happens only after successful execution. */
public interface GenerationArtifactStaging extends AutoCloseable {
    GenerationArtifactStaging NONE = new GenerationArtifactStaging() {
        @Override public Path directory() { return null; }
        @Override public List<GenerationArtifact> promote(List<GenerationArtifact> artifacts) {
            return artifacts == null ? List.of() : List.copyOf(artifacts);
        }
        @Override public void close() { }
    };

    Path directory();
    List<GenerationArtifact> promote(List<GenerationArtifact> artifacts);
    @Override void close();
}
