package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

/** A visible step in the guided model installation assistant. */
public record ModelInstallStep(String title, String detail) {
    public ModelInstallStep {
        title = require(title, "title");
        detail = require(detail, "detail");
    }

    private static String require(String value, String name) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return normalized;
    }
}
