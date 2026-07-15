package com.marcosmoreiradev.docupodcaststudio.application.project;

import java.util.List;

/** Validation result for a persisted project payload. */
public record ProjectValidationResult(boolean valid, List<String> messages) {
    public ProjectValidationResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static ProjectValidationResult ok() {
        return new ProjectValidationResult(true, List.of());
    }

    public static ProjectValidationResult invalid(List<String> messages) {
        return new ProjectValidationResult(false, messages);
    }

    public void throwIfInvalid() {
        if (!valid) {
            throw new IllegalStateException(String.join("; ", messages));
        }
    }
}
