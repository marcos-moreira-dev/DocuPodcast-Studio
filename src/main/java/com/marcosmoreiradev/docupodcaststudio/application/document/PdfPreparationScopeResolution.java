package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Pages selected for a scope, or an explicit request for a manual range. */
public record PdfPreparationScopeResolution(
        PdfPreparationScope scope,
        List<Integer> pages,
        boolean requiresManualRange,
        String explanation
) {
    public PdfPreparationScopeResolution {
        scope = scope == null ? PdfPreparationScope.CURRENT_PAGE : scope;
        pages = pages == null ? List.of() : List.copyOf(pages);
        explanation = explanation == null ? "" : explanation.strip();
    }
}
