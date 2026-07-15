package com.marcosmoreiradev.docupodcaststudio.domain.project;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable metadata persisted in the root .docupodcast.json file.
 */
public record ProjectMetadata(
        String id,
        String title,
        String description,
        String language,
        ProjectKind kind,
        ProjectMode mode,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public ProjectMetadata(
            String id,
            String title,
            String description,
            String language,
            ProjectKind kind,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, title, description, language, kind, ProjectMode.defaultMode(), status, createdAt, updatedAt);
    }

    public ProjectMetadata {
        id = requireToken(id, "id");
        title = requireText(title, "title");
        description = normalizeOptional(description);
        language = normalizeLanguage(language);
        kind = Objects.requireNonNullElse(kind, ProjectKind.EMPTY);
        mode = Objects.requireNonNullElse(mode, ProjectMode.defaultMode());
        status = Objects.requireNonNullElse(status, ProjectStatus.DRAFT);
        createdAt = Objects.requireNonNullElse(createdAt, Instant.now());
        updatedAt = Objects.requireNonNullElse(updatedAt, createdAt);
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static ProjectMetadata create(String title) {
        Instant now = Instant.now();
        return new ProjectMetadata(
                "PRJ-" + UUID.randomUUID(),
                title,
                "",
                "es",
                ProjectKind.EMPTY,
                ProjectMode.defaultMode(),
                ProjectStatus.DRAFT,
                now,
                now
        );
    }

    public static ProjectMetadata create(String title, ProjectMode mode) {
        return create(title).withMode(mode);
    }

    public ProjectMetadata withKind(ProjectKind newKind) {
        return new ProjectMetadata(id, title, description, language, newKind, mode, status, createdAt, Instant.now());
    }

    public ProjectMetadata withMode(ProjectMode newMode) {
        return new ProjectMetadata(id, title, description, language, kind, newMode, status, createdAt, Instant.now());
    }

    public ProjectMetadata withStatus(ProjectStatus newStatus) {
        return new ProjectMetadata(id, title, description, language, kind, mode, newStatus, createdAt, Instant.now());
    }

    public ProjectMetadata withTitle(String newTitle) {
        return new ProjectMetadata(id, newTitle, description, language, kind, mode, status, createdAt, Instant.now());
    }

    private static String requireText(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String requireToken(String value, String field) {
        String normalized = requireText(value, field);
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeLanguage(String value) {
        String normalized = normalizeOptional(value).toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "es" : normalized;
    }
}
