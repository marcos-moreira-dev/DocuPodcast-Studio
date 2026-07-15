package com.marcosmoreiradev.docupodcaststudio.domain.storyboard;

/** Validation issue for storyboard/segment/image consistency. */
public record StoryboardValidationIssue(String level, String message, String referenceId) {
    public StoryboardValidationIssue {
        level = level == null || level.isBlank() ? "WARNING" : level.strip().toUpperCase(java.util.Locale.ROOT);
        message = message == null ? "" : message.strip();
        referenceId = referenceId == null ? "" : referenceId.strip();
        if (message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
