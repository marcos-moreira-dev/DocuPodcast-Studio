package com.marcosmoreiradev.docupodcaststudio.application.document;

/** User-facing counts explaining what was read, omitted or still needs review. */
public record PdfReviewDashboard(
        int narratableRegions,
        int omittedRegions,
        int pendingRegions,
        int draftTreatments,
        int failedAttempts,
        int truncatedAttempts,
        int insufficientEvidenceAttempts
) {
    public String summary() {
        return "Listo para leer: " + narratableRegions
                + " · omitido: " + omittedRegions
                + " · pendiente: " + pendingRegions
                + " · borradores: " + draftTreatments
                + " · fallos: " + failedAttempts
                + " · truncados: " + truncatedAttempts
                + " · evidencia insuficiente: " + insufficientEvidenceAttempts;
    }
}
