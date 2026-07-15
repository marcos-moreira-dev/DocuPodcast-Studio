package com.marcosmoreiradev.docupodcaststudio.domain.script;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal compatibility payload for the prepared reading generated from a source document.
 *
 * <p>This record is still used by audio, render, playback and persisted project round-trip code.
 * It must not be presented to the user as a separate "guion" or upload category.</p>
 */
public record NarrationScriptDocument(
        String id,
        String title,
        String language,
        String sourceDocumentTitle,
        List<NarrationSegment> segments,
        Instant createdAt,
        Instant updatedAt,
        String notes
) {
    public NarrationScriptDocument {
        id = normalizeToken(id, "SCRIPT-001");
        title = normalizeTitle(title);
        language = normalizeLanguage(language);
        sourceDocumentTitle = sourceDocumentTitle == null ? "" : sourceDocumentTitle.strip();
        segments = segments == null ? List.of() : List.copyOf(segments);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        notes = notes == null ? "" : notes.strip();
        validateUniqueSegments(segments);
    }

    public static NarrationScriptDocument create(String title, String language, String sourceDocumentTitle, List<NarrationSegment> segments) {
        Instant now = Instant.now();
        return new NarrationScriptDocument("SCRIPT-001", title, language, sourceDocumentTitle, segments, now, now,
                "Lectura preparada desde el documento importado. Revisa fragmentos antes de generar audio.");
    }

    public Optional<NarrationSegment> segmentById(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return Optional.empty();
        }
        return segments.stream().filter(segment -> segment.id().equals(segmentId.strip())).findFirst();
    }

    public NarrationScriptDocument replaceSegment(NarrationSegment replacement) {
        Objects.requireNonNull(replacement, "replacement");
        ArrayList<NarrationSegment> next = new ArrayList<>(segments.size());
        boolean found = false;
        for (NarrationSegment segment : segments) {
            if (segment.id().equals(replacement.id())) {
                next.add(replacement);
                found = true;
            } else {
                next.add(segment);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("No existe el segmento " + replacement.id());
        }
        return new NarrationScriptDocument(id, title, language, sourceDocumentTitle, next, createdAt, Instant.now(), notes);
    }

    public int segmentCount() {
        return segments.size();
    }

    public long narratableSegmentCount() {
        return segments.stream().filter(NarrationSegment::narratable).count();
    }

    public long wordCount() {
        return segments.stream().mapToLong(NarrationSegment::wordCount).sum();
    }

    public long estimatedCharacters() {
        return segments.stream().mapToLong(NarrationSegment::characterCount).sum();
    }

    public boolean empty() {
        return segments.isEmpty();
    }

    private static void validateUniqueSegments(List<NarrationSegment> segments) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (NarrationSegment segment : segments) {
            if (!ids.add(segment.id())) {
                throw new IllegalArgumentException("Segmento duplicado: " + segment.id());
            }
        }
    }

    private static String normalizeToken(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("id must not contain whitespace");
        }
        return normalized;
    }

    private static String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.strip();
        return normalized.isBlank() ? "Lectura preparada" : normalized;
    }

    private static String normalizeLanguage(String language) {
        String normalized = language == null ? "" : language.strip().toLowerCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "es" : normalized;
    }
}
