package com.marcosmoreiradev.docupodcaststudio.domain.render;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Ordered unit-level narration plan derived from a script and project layers. */
public record NarrationRenderPlan(
        String id,
        String scriptId,
        List<NarrationRenderUnit> units,
        Instant createdAt
) {
    public NarrationRenderPlan {
        id = token(id, "id");
        scriptId = token(scriptId, "scriptId");
        units = units == null ? List.of() : List.copyOf(units);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        validate(units);
    }

    public boolean empty() {
        return units.isEmpty();
    }

    public int unitCount() {
        return units.size();
    }

    public long ttsUnitCount() {
        return units.stream().filter(NarrationRenderUnit::usesTts).count();
    }

    public long audioClipUnitCount() {
        return units.stream().filter(NarrationRenderUnit::usesAudioClip).count();
    }

    public List<NarrationRenderUnit> unitsForSegment(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return List.of();
        }
        String target = segmentId.strip();
        return units.stream().filter(unit -> unit.segmentId().equals(target)).toList();
    }

    public Optional<NarrationRenderUnit> unitById(String unitId) {
        if (unitId == null || unitId.isBlank()) {
            return Optional.empty();
        }
        String target = unitId.strip();
        return units.stream().filter(unit -> unit.id().equals(target)).findFirst();
    }

    private static void validate(List<NarrationRenderUnit> units) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (NarrationRenderUnit unit : units) {
            if (!ids.add(unit.id())) {
                throw new IllegalArgumentException("Unidad de render duplicada: " + unit.id());
            }
        }
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }
}
