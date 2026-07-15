package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorStatus;

import java.util.List;

/** Summary of project-side text anchor reconciliation after re-importing a source document. */
public record TextAnchorReconciliationReport(
        List<TextAnchorReconciliationEntry> entries
) {
    public TextAnchorReconciliationReport {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public long currentCount() {
        return count(TextAnchorStatus.CURRENT);
    }

    public long relocatedCount() {
        return count(TextAnchorStatus.RELOCATED);
    }

    public long needsReviewCount() {
        return count(TextAnchorStatus.NEEDS_REVIEW);
    }

    public long orphanedCount() {
        return count(TextAnchorStatus.ORPHANED);
    }

    public List<NarrativeLayerAssignment> reconciledAssignments() {
        return entries.stream()
                .map(TextAnchorReconciliationEntry::reconciledAssignment)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public boolean hasRisk() {
        return needsReviewCount() + orphanedCount() > 0;
    }

    private long count(TextAnchorStatus status) {
        return entries.stream().filter(entry -> entry.status() == status).count();
    }
}
