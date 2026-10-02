package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable request for preparing exactly one PDF page. */
public record PreparePdfPageRequest(
        PreparedPdfWorkspaceRef workspace,
        Path cacheDirectory,
        int pageNumber,
        boolean forceOcr,
        PdfPreparationCancellationToken cancellationToken,
        PdfPreparationPriority priority,
        ProgressSink progress,
        PdfPreparationOrigin origin,
        boolean retryAllowed,
        String scopeId,
        com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy readingStrategy,
        com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNativeTextProvider nativeTextProvider
) {
    public PreparePdfPageRequest {
        workspace = Objects.requireNonNull(workspace, "workspace");
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber must be 1-based");
        }
        cancellationToken = cancellationToken == null ? PdfPreparationCancellationToken.NONE : cancellationToken;
        priority = priority == null ? PdfPreparationPriority.NORMAL : priority;
        progress = progress == null ? ProgressSink.NONE : progress;
        origin = origin == null ? PdfPreparationOrigin.PROCESS_COMPLETE : origin;
        retryAllowed = retryAllowed && origin.retryAuthority();
        scopeId = scopeId == null || scopeId.isBlank() ? "standalone" : scopeId.strip();
    }

    public PreparePdfPageRequest(PreparedPdfWorkspaceRef workspace, Path cacheDirectory, int pageNumber,
                                 boolean forceOcr, PdfPreparationCancellationToken cancellationToken,
                                 PdfPreparationPriority priority, ProgressSink progress,
                                 PdfPreparationOrigin origin, boolean retryAllowed, String scopeId) {
        this(workspace, cacheDirectory, pageNumber, forceOcr, cancellationToken, priority,
                progress, origin, retryAllowed, scopeId, null, null);
    }

    public PreparePdfPageRequest(
            PreparedPdfWorkspaceRef workspace,
            Path cacheDirectory,
            int pageNumber,
            boolean forceOcr,
            PdfPreparationCancellationToken cancellationToken,
            PdfPreparationPriority priority,
            ProgressSink progress) {
        this(workspace, cacheDirectory, pageNumber, forceOcr,
                cancellationToken, priority, progress,
                PdfPreparationOrigin.PROCESS_COMPLETE, true, "standalone");
    }

    public PreparePdfPageRequest(
            PreparedPdfWorkspaceRef workspace,
            Path cacheDirectory,
            int pageNumber,
            boolean forceOcr,
            PdfPreparationCancellationToken cancellationToken,
            PdfPreparationPriority priority) {
        this(workspace, cacheDirectory, pageNumber, forceOcr,
                cancellationToken, priority, ProgressSink.NONE,
                PdfPreparationOrigin.PROCESS_COMPLETE, true, "standalone");
    }

    public PreparePdfPageRequest(
            PreparedPdfWorkspaceRef workspace,
            Path cacheDirectory,
            int pageNumber,
            boolean forceOcr,
            PdfPreparationCancellationToken cancellationToken) {
        this(workspace, cacheDirectory, pageNumber, forceOcr,
                cancellationToken, PdfPreparationPriority.NORMAL,
                ProgressSink.NONE, PdfPreparationOrigin.PROCESS_COMPLETE,
                true, "standalone");
    }
}
