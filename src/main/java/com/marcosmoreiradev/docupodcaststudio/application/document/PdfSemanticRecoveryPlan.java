package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** At most one bounded targeted-recovery round for a page. */
public record PdfSemanticRecoveryPlan(List<PdfSemanticRecoveryRoi> rois) {
    public static final int MAX_ROIS = 3;

    public PdfSemanticRecoveryPlan {
        rois = rois == null ? List.of() : rois.stream().distinct()
                .limit(MAX_ROIS).toList();
    }

    public boolean empty() {
        return rois.isEmpty();
    }
}
