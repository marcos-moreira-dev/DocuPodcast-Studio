package com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage;

import java.util.List;
import java.util.Objects;

/** Immutable, user-reviewable delta plan. */
public record TheatreRefreshPlan(
        String packageId,
        String previousFingerprint,
        String currentFingerprint,
        List<TheatrePackageDelta> deltas
) {
    public TheatreRefreshPlan {
        packageId = Objects.requireNonNullElse(packageId, "").strip();
        previousFingerprint = Objects.requireNonNullElse(previousFingerprint, "").strip();
        currentFingerprint = Objects.requireNonNullElse(currentFingerprint, "").strip();
        deltas = deltas == null ? List.of() : List.copyOf(deltas);
    }

    public long count(TheatrePackageDeltaStatus status) {
        return deltas.stream().filter(delta -> delta.status() == status).count();
    }

    public boolean hasConflicts() { return count(TheatrePackageDeltaStatus.CONFLICT) > 0; }

    public boolean hasEffectiveChanges() {
        return deltas.stream().anyMatch(delta -> switch (delta.status()) {
            case NEW, MODIFIED, RENAMED -> true;
            default -> false;
        });
    }

    public boolean isNoOp() { return !hasEffectiveChanges() && !hasConflicts(); }
}
