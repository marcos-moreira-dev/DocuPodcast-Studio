package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Enforces one main image and one optional bridge image per narrative fragment. */
public final class NarrativeVisualSlotPolicy {
    public List<NarrativeLayerAssignment> replaceSlot(
            List<NarrativeLayerAssignment> assignments,
            NarrativeLayerAssignment candidate
    ) {
        Objects.requireNonNull(candidate, "candidate");
        if (!visualSlotKind(candidate.kind())) {
            throw new IllegalArgumentException("Only IMAGE or BRIDGE_IMAGE can be applied as visual slots.");
        }
        ArrayList<NarrativeLayerAssignment> updated = new ArrayList<>();
        for (NarrativeLayerAssignment existing : assignments == null ? List.<NarrativeLayerAssignment>of() : assignments) {
            if (!sameSlot(existing, candidate)) {
                updated.add(existing);
            }
        }
        updated.add(candidate);
        return List.copyOf(updated);
    }

    public boolean hasSlot(List<NarrativeLayerAssignment> assignments, String segmentId, NarrativeLayerKind kind) {
        String targetSegment = normalize(segmentId);
        if (targetSegment.isBlank() || !visualSlotKind(kind)) {
            return false;
        }
        return (assignments == null ? List.<NarrativeLayerAssignment>of() : assignments).stream()
                .anyMatch(assignment -> assignment.kind() == kind
                        && assignment.textRange().segmentId().equals(targetSegment));
    }

    public static boolean visualSlotKind(NarrativeLayerKind kind) {
        return kind == NarrativeLayerKind.IMAGE || kind == NarrativeLayerKind.BRIDGE_IMAGE;
    }

    private static boolean sameSlot(NarrativeLayerAssignment existing, NarrativeLayerAssignment candidate) {
        return existing != null
                && existing.kind() == candidate.kind()
                && existing.textRange().segmentId().equals(candidate.textRange().segmentId());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
