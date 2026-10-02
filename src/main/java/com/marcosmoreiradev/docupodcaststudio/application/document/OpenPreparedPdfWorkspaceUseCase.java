package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

/** Resolves and queries a canonical PDF V2 workspace without block projections. */
public final class OpenPreparedPdfWorkspaceUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final PdfOperationAttemptRepository attempts;

    public PdfReadingPreferencesUseCase readingPreferences() {
        return new PdfReadingPreferencesUseCase(repository);
    }

    public OpenPreparedPdfWorkspaceUseCase(PreparedPdfDocumentRepository repository) {
        this(repository, PdfOperationAttemptRepository.disabled());
    }

    public OpenPreparedPdfWorkspaceUseCase(PreparedPdfDocumentRepository repository,
                                           PdfOperationAttemptRepository attempts) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.attempts = Objects.requireNonNullElseGet(
                attempts, PdfOperationAttemptRepository::disabled);
    }

    public Optional<PreparedPdfWorkspaceRef> open(Path sourcePath) {
        Path root = PreparePdfPageUseCase.projectRoot(sourcePath);
        if (root == null || sourcePath == null) return Optional.empty();
        try {
            return repository.loadManifest(root).map(manifest ->
                    new PreparedPdfWorkspaceRef(root, sourcePath, manifest.sourceSha256()));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public int pageCount(PreparedPdfWorkspaceRef workspace) {
        try {
            return repository.loadManifest(workspace.projectRoot())
                    .filter(manifest -> manifest.sourceSha256().equalsIgnoreCase(workspace.sourceSha256()))
                    .map(manifest -> manifest.pageCount())
                    .orElse(0);
        } catch (IOException ex) {
            return 0;
        }
    }

    public boolean pagePrepared(PreparedPdfWorkspaceRef workspace, int pageNumber) {
        try {
            return pageNumber > 0 && repository.loadPage(workspace.projectRoot(), pageNumber).isPresent();
        } catch (IOException ex) {
            return false;
        }
    }

    /** Reads canonical/attempt persistence only; it never schedules preparation. */
    public PdfPageViewerState pageViewerState(PreparedPdfWorkspaceRef workspace,
                                              int pageNumber) {
        if (workspace == null || pageNumber <= 0) return PdfPageViewerState.NOT_PREPARED;
        if (pagePrepared(workspace, pageNumber)) return PdfPageViewerState.PREPARED;
        try {
            return attempts.list(workspace.projectRoot()).stream()
                    .filter(attempt -> attempt.pageNumber() == pageNumber)
                    .filter(attempt -> "PAGE_SEMANTIC_READING".equals(attempt.operation()))
                    .max(java.util.Comparator.comparing(
                            com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                                    .PdfOperationAttempt::finishedAt))
                    .map(attempt -> switch (attempt.state()) {
                        case FAILED, TRUNCATED, INTERRUPTED -> PdfPageViewerState.TECHNICAL_FAILURE;
                        case IN_FLIGHT -> PdfPageViewerState.NOT_PREPARED;
                        case INSUFFICIENT_EVIDENCE -> PdfPageViewerState.SEMANTIC_REJECTION;
                        case CANCELLED -> PdfPageViewerState.CANCELLED;
                        case COMPLETED -> PdfPageViewerState.NOT_PREPARED;
                    }).orElse(PdfPageViewerState.NOT_PREPARED);
        } catch (IOException failure) {
            return PdfPageViewerState.NOT_PREPARED;
        }
    }

    public int narratableRegionCount(PreparedPdfWorkspaceRef workspace, int pageNumber) {
        try {
            return repository.loadPage(workspace.projectRoot(), pageNumber)
                    .map(page -> (int) page.regions().stream()
                            .filter(region -> region.effectiveNarratability() == PdfNarratability.NARRATABLE)
                            .count())
                    .orElse(0);
        } catch (IOException ex) {
            return 0;
        }
    }

    /** Read-only access for format-neutral projections and video ROI materialization. */
    public Optional<PreparedPdfPage> loadPage(PreparedPdfWorkspaceRef workspace, int pageNumber) {
        if (workspace == null || pageNumber <= 0) return Optional.empty();
        try {
            return repository.loadPage(workspace.projectRoot(), pageNumber);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    /** Returns only already prepared pages; this never invokes OCR or local AI. */
    public List<PreparedPdfPage> loadPreparedPages(PreparedPdfWorkspaceRef workspace) {
        if (workspace == null) return List.of();
        try {
            return repository.loadPages(workspace.projectRoot());
        } catch (IOException ex) {
            return List.of();
        }
    }
}
