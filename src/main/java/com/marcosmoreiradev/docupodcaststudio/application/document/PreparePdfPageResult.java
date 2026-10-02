package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfOperationAttemptState;

import java.util.List;

/** Outcome of a single-page preparation attempt. */
public record PreparePdfPageResult(
        int pageNumber,
        PreparedPdfPage preparedPage,
        boolean changed,
        boolean cancelled,
        List<DocumentImportIssue> issues,
        PdfOperationAttemptState outcomeState,
        String failureCategory,
        String userFacingReason,
        Throwable diagnosticCause
) {
    public PreparePdfPageResult {
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        issues = issues == null ? List.of() : List.copyOf(issues);
        outcomeState = outcomeState == null
                ? inferState(preparedPage, cancelled) : outcomeState;
        failureCategory = failureCategory == null ? "" : failureCategory.strip();
        userFacingReason = userFacingReason == null ? "" : userFacingReason.strip();
    }

    /** Compatibility constructor for standard/native/OCR callers. */
    public PreparePdfPageResult(int pageNumber, PreparedPdfPage preparedPage,
                                boolean changed, boolean cancelled,
                                List<DocumentImportIssue> issues) {
        this(pageNumber, preparedPage, changed, cancelled, issues,
                inferState(preparedPage, cancelled), "", "", null);
    }

    public boolean succeeded() {
        return outcomeState == PdfOperationAttemptState.COMPLETED
                && !cancelled && preparedPage != null;
    }

    public boolean rejected() {
        return outcomeState == PdfOperationAttemptState.INSUFFICIENT_EVIDENCE;
    }

    public boolean technicalFailure() {
        return outcomeState == PdfOperationAttemptState.FAILED
                || outcomeState == PdfOperationAttemptState.TRUNCATED
                || outcomeState == PdfOperationAttemptState.INTERRUPTED;
    }

    public boolean canonicalPageAvailable() {
        return preparedPage != null;
    }

    public String terminalMessage() {
        if (succeeded()) return "Página " + pageNumber + " preparada.";
        if (cancelled || outcomeState == PdfOperationAttemptState.CANCELLED) {
            return "Preparación de la página " + pageNumber + " cancelada.";
        }
        if (rejected()) {
            return "Página " + pageNumber
                    + " no pudo interpretarse con suficiente fiabilidad.";
        }
        return "No se pudo procesar la página " + pageNumber + ".";
    }

    public static PreparePdfPageResult rejected(
            int pageNumber, PreparedPdfPage previous, List<DocumentImportIssue> issues,
            String reason, Throwable cause) {
        return new PreparePdfPageResult(pageNumber, previous, false, false, issues,
                PdfOperationAttemptState.INSUFFICIENT_EVIDENCE,
                "INSUFFICIENT_EVIDENCE", reason, cause);
    }

    public static PreparePdfPageResult technicalFailure(
            int pageNumber, PreparedPdfPage previous, List<DocumentImportIssue> issues,
            PdfOperationAttemptState state, String category, String reason,
            Throwable cause) {
        PdfOperationAttemptState safe = state == PdfOperationAttemptState.TRUNCATED
                ? state : PdfOperationAttemptState.FAILED;
        return new PreparePdfPageResult(pageNumber, previous, false, false, issues,
                safe, category, reason, cause);
    }

    public static PreparePdfPageResult cancelled(int pageNumber,
                                                  PreparedPdfPage previous) {
        return new PreparePdfPageResult(pageNumber, previous, false, true, List.of(),
                PdfOperationAttemptState.CANCELLED, "CANCELLED",
                "Preparación cancelada.", null);
    }

    private static PdfOperationAttemptState inferState(
            PreparedPdfPage page, boolean cancelled) {
        if (cancelled) return PdfOperationAttemptState.CANCELLED;
        return page == null ? PdfOperationAttemptState.FAILED
                : PdfOperationAttemptState.COMPLETED;
    }
}
