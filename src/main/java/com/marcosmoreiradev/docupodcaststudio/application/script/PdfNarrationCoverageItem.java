package com.marcosmoreiradev.docupodcaststudio.application.script;

import java.util.List;

public record PdfNarrationCoverageItem(
        int pageNumber,
        String regionId,
        int readingOrder,
        String regionType,
        PdfNarrationCoverageStatus status,
        List<String> narrationSegmentIds,
        String reason
) {
    public PdfNarrationCoverageItem {
        narrationSegmentIds = narrationSegmentIds == null ? List.of() : List.copyOf(narrationSegmentIds);
        reason = reason == null ? "" : reason.strip();
    }
}
