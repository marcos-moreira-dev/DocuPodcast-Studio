package com.marcosmoreiradev.docupodcaststudio.application.script;

import java.util.List;

public record PdfNarrationCoverageReport(List<PdfNarrationCoverageItem> items) {
    public PdfNarrationCoverageReport {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public boolean complete() {
        return items.stream().noneMatch(item -> item.status() == PdfNarrationCoverageStatus.INCOMPLETE);
    }

    public List<PdfNarrationCoverageItem> incompleteItems() {
        return items.stream().filter(item -> item.status() == PdfNarrationCoverageStatus.INCOMPLETE).toList();
    }
}
