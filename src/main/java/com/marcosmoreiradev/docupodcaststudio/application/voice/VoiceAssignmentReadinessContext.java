package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.nio.file.Path;
import java.util.Objects;

/** Filesystem roots required to prove that a referenced voice sample can be used now. */
public record VoiceAssignmentReadinessContext(
        Path installationRoot,
        Path runtimeRoot,
        Path projectRoot
) {
    public VoiceAssignmentReadinessContext {
        installationRoot = normalize(installationRoot, "installationRoot");
        runtimeRoot = normalize(runtimeRoot, "runtimeRoot");
        projectRoot = projectRoot == null
                ? runtimeRoot
                : projectRoot.toAbsolutePath().normalize();
    }

    public VoiceReferenceSamplePathResolver samplePathResolver() {
        return new VoiceReferenceSamplePathResolver(installationRoot, runtimeRoot);
    }

    private static Path normalize(Path path, String field) {
        return Objects.requireNonNull(path, field).toAbsolutePath().normalize();
    }
}
