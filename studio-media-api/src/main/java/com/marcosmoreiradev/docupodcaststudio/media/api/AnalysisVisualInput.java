package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Files;
import java.nio.file.Path;

/** A visual input with an explicit semantic role; it is not PDF-specific. */
public record AnalysisVisualInput(Path file, String role, String mediaType) {
    public AnalysisVisualInput {
        if (file == null) throw new IllegalArgumentException("analysis input file is required");
        file = file.toAbsolutePath().normalize();
        role = role == null || role.isBlank() ? "primary" : role.strip();
        mediaType = mediaType == null || mediaType.isBlank() ? infer(file) : mediaType.strip();
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    private static String infer(Path file) {
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/png";
    }
}
