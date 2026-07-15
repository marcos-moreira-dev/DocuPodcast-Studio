package com.marcosmoreiradev.docupodcaststudio.domain.assignment;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Protection rules for assigning production layers to text. A range can have many
 * compatible layers, but only one primary narration source unless the user replaces
 * or splits the existing assignment explicitly.
 */
public final class NarrativeLayerAssignmentPolicy {
    private NarrativeLayerAssignmentPolicy() {
    }

    public static boolean compatible(NarrativeLayerAssignment existing, NarrativeLayerAssignment candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (existing == null || !existing.overlaps(candidate)) {
            return true;
        }
        if (existing.primaryNarrationLayer() && candidate.primaryNarrationLayer()) {
            return false;
        }
        if (existing.kind() == candidate.kind() && !candidate.kind().stackableLayer()) {
            return false;
        }
        return true;
    }

    public static List<NarrativeLayerAssignment> conflicts(
            Collection<NarrativeLayerAssignment> existing,
            NarrativeLayerAssignment candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (existing == null || existing.isEmpty()) {
            return List.of();
        }
        return existing.stream()
                .filter(assignment -> !compatible(assignment, candidate))
                .toList();
    }

    public static String conflictMessage(NarrativeLayerAssignment existing, NarrativeLayerAssignment candidate) {
        if (compatible(existing, candidate)) {
            return "Sin conflicto";
        }
        if (existing.primaryNarrationLayer() && candidate.primaryNarrationLayer()) {
            return "Ese rango ya tiene una voz o audio principal. Reemplaza, divide o desasigna antes de aplicar otra fuente principal.";
        }
        return "Ese rango ya tiene una asignación incompatible del mismo tipo.";
    }
}
