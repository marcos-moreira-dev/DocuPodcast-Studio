package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/** Resolves a semantic scope to a deterministic page list. */
public record PdfPreparationScopeRequest(
        PreparedPdfWorkspaceRef workspace,
        PdfPreparationScope scope,
        int currentPage,
        int rangeStart,
        int rangeEnd,
        DocumentOutlineProjection outline,
        PdfPreparationOrigin origin,
        boolean retryAllowed,
        String scopeId
) {
    public PdfPreparationScopeRequest {
        workspace = Objects.requireNonNull(workspace, "workspace");
        scope = scope == null ? PdfPreparationScope.CURRENT_PAGE : scope;
        currentPage = Math.max(1, currentPage);
        rangeStart = Math.max(0, rangeStart);
        rangeEnd = Math.max(0, rangeEnd);
        origin = origin == null ? PdfPreparationOrigin.PROCESS_COMPLETE : origin;
        retryAllowed = retryAllowed && origin.retryAuthority();
        scopeId = scopeId == null || scopeId.isBlank()
                ? java.util.UUID.randomUUID().toString() : scopeId.strip();
    }

    public PdfPreparationScopeRequest(PreparedPdfWorkspaceRef workspace,
                                      PdfPreparationScope scope,
                                      int currentPage,
                                      int rangeStart,
                                      int rangeEnd,
                                      DocumentOutlineProjection outline) {
        this(workspace, scope, currentPage, rangeStart, rangeEnd, outline,
                PdfPreparationOrigin.PROCESS_COMPLETE, true, null);
    }
}
