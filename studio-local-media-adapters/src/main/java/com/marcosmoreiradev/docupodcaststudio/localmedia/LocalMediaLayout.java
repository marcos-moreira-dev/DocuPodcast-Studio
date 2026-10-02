package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable deployment layout supplied by the production composition root. */
public record LocalMediaLayout(Path installationRoot, Path runtimeRoot) {
    public LocalMediaLayout {
        installationRoot = normalize(installationRoot, "installation root");
        runtimeRoot = normalize(runtimeRoot, "runtime root");
    }

    public static LocalMediaLayout development(Path repositoryRoot) {
        Path root = normalize(repositoryRoot, "repository root");
        return new LocalMediaLayout(root, root);
    }

    private static Path normalize(Path value, String label) {
        return Objects.requireNonNull(value, label).toAbsolutePath().normalize();
    }
}
