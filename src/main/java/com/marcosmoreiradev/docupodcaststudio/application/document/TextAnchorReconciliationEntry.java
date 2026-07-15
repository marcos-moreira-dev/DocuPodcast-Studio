package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorStatus;

/** One layer-anchor reconciliation decision after a source document refresh. */
public record TextAnchorReconciliationEntry(
        String assignmentId,
        TextAnchor previousAnchor,
        TextAnchor reconciledAnchor,
        NarrativeLayerAssignment reconciledAssignment,
        String reason
) {
    public TextAnchorReconciliationEntry {
        assignmentId = assignmentId == null ? "" : assignmentId.strip();
        reason = reason == null ? "" : reason.strip();
    }

    public TextAnchorStatus status() {
        return reconciledAnchor == null ? TextAnchorStatus.NEEDS_REVIEW : reconciledAnchor.status();
    }

    public boolean changed() {
        return previousAnchor != null && reconciledAnchor != null && !previousAnchor.equals(reconciledAnchor);
    }
}
