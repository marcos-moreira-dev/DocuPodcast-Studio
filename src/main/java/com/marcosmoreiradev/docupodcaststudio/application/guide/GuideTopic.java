package com.marcosmoreiradev.docupodcaststudio.application.guide;

import java.util.Objects;

/** A self-contained help/guide topic rendered by the integrated desktop guide. */
public record GuideTopic(
        GuideTopicId id,
        String category,
        String title,
        String summary,
        String bodyMarkdown
) {
    public GuideTopic {
        Objects.requireNonNull(id, "id");
        category = normalize(category, "General");
        title = normalize(title, id.name());
        summary = normalize(summary, "");
        bodyMarkdown = normalize(bodyMarkdown, "");
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
