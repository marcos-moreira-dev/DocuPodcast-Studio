package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

/** Display metadata for the theatre AI module navigation. */
public record TheatreAiModuleDescriptor(
        TheatreAiModuleId id,
        String group,
        String title,
        String description
) {
    public TheatreAiModuleDescriptor {
        id = id == null ? TheatreAiModuleId.HOME : id;
        group = clean(group, "OPERACION");
        title = clean(title, id.title());
        description = clean(description, id.description());
    }

    public static TheatreAiModuleDescriptor of(TheatreAiModuleId id, String group) {
        return new TheatreAiModuleDescriptor(id, group, id.title(), id.description());
    }

    private static String clean(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
