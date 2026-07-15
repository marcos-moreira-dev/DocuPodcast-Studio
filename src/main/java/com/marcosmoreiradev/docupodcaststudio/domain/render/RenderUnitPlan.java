package com.marcosmoreiradev.docupodcaststudio.domain.render;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Cross-media plan that classifies each effective unit for audio, storyboard and video. */
public record RenderUnitPlan(
        String id,
        String sourceScriptId,
        List<RenderUnit> units,
        double defaultSilentVisualDurationSeconds,
        Instant createdAt
) {
    public RenderUnitPlan {
        id = token(id, "id");
        sourceScriptId = normalize(sourceScriptId);
        units = units == null ? List.of() : List.copyOf(units);
        defaultSilentVisualDurationSeconds = defaultSilentVisualDurationSeconds <= 0
                ? 5.0
                : Math.max(1.0, Math.min(60.0, defaultSilentVisualDurationSeconds));
        createdAt = createdAt == null ? Instant.now() : createdAt;
        validate(units);
    }

    public int unitCount() {
        return units.size();
    }

    public long spokenUnitCount() {
        return units.stream().filter(unit -> unit.kind().spoken()).count();
    }

    public long visualUnitCount() {
        return units.stream().filter(RenderUnit::renderableInVideo).count();
    }

    public long silentVisualUnitCount() {
        return units.stream().filter(RenderUnit::silentVideoFrame).count();
    }

    public long omittedFromVideoCount() {
        return units.stream().filter(RenderUnit::omittedFromVideo).count();
    }

    public List<RenderUnit> audioUnits() {
        return units.stream().filter(unit -> unit.kind().spoken()).toList();
    }

    public List<RenderUnit> videoUnits() {
        return units.stream().filter(RenderUnit::renderableInVideo).toList();
    }

    public Optional<RenderUnit> unitById(String id) {
        String target = normalize(id);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return units.stream().filter(unit -> unit.id().equals(target)).findFirst();
    }

    private static void validate(List<RenderUnit> units) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (RenderUnit unit : units) {
            if (!ids.add(unit.id())) {
                throw new IllegalArgumentException("RenderUnit duplicada: " + unit.id());
            }
        }
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
