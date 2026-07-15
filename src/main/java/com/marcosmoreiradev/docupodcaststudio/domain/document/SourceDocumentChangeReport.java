package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.util.Objects;

/**
 * Comparison report produced when the user asks to refresh a read-only source document.
 * It does not mutate derived artifacts; it tells the app which ones need regeneration or review.
 */
public record SourceDocumentChangeReport(
        SourceDocumentChangeStatus status,
        SourceDocumentSnapshot previousSnapshot,
        SourceDocumentSnapshot currentSnapshot,
        DerivedArtifactFreshness audioFreshness,
        DerivedArtifactFreshness layersFreshness,
        DerivedArtifactFreshness storyboardFreshness,
        String summary
) {
    public SourceDocumentChangeReport {
        status = Objects.requireNonNull(status, "status");
        previousSnapshot = Objects.requireNonNull(previousSnapshot, "previousSnapshot");
        audioFreshness = Objects.requireNonNull(audioFreshness, "audioFreshness");
        layersFreshness = Objects.requireNonNull(layersFreshness, "layersFreshness");
        storyboardFreshness = Objects.requireNonNull(storyboardFreshness, "storyboardFreshness");
        summary = summary == null || summary.isBlank() ? defaultSummary(status) : summary.strip();
    }

    public static SourceDocumentChangeReport compare(SourceDocumentSnapshot previous, SourceDocumentSnapshot current) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        if (previous.sameContentAs(current)) {
            return new SourceDocumentChangeReport(
                    SourceDocumentChangeStatus.UNCHANGED,
                    previous,
                    current,
                    DerivedArtifactFreshness.CURRENT,
                    DerivedArtifactFreshness.CURRENT,
                    DerivedArtifactFreshness.CURRENT,
                    "El documento fuente no cambió; audio, capas y storyboard siguen vigentes.");
        }
        return new SourceDocumentChangeReport(
                SourceDocumentChangeStatus.CHANGED,
                previous,
                current,
                DerivedArtifactFreshness.STALE,
                DerivedArtifactFreshness.REVIEW_REQUIRED,
                DerivedArtifactFreshness.REVIEW_REQUIRED,
                "El documento fuente cambió; el audio queda obsoleto y las capas/storyboard requieren revisión.");
    }

    public static SourceDocumentChangeReport missing(SourceDocumentSnapshot previous) {
        Objects.requireNonNull(previous, "previous");
        return new SourceDocumentChangeReport(
                SourceDocumentChangeStatus.MISSING,
                previous,
                null,
                DerivedArtifactFreshness.UNKNOWN,
                DerivedArtifactFreshness.REVIEW_REQUIRED,
                DerivedArtifactFreshness.REVIEW_REQUIRED,
                "El archivo fuente ya no está disponible; no se modifica el proyecto y se requiere revisión manual.");
    }


    public static SourceDocumentChangeReport unsupported(SourceDocumentSnapshot previous, String reason) {
        Objects.requireNonNull(previous, "previous");
        String detail = reason == null || reason.isBlank()
                ? "El documento fuente no cumple los requerimientos de DocuPodcast Studio V1."
                : reason.strip();
        return new SourceDocumentChangeReport(
                SourceDocumentChangeStatus.UNSUPPORTED,
                previous,
                null,
                DerivedArtifactFreshness.UNKNOWN,
                DerivedArtifactFreshness.REVIEW_REQUIRED,
                DerivedArtifactFreshness.REVIEW_REQUIRED,
                detail);
    }

    public int blockCountDelta() {
        return currentSnapshot == null ? 0 : currentSnapshot.blockCount() - previousSnapshot.blockCount();
    }

    public long wordCountDelta() {
        return currentSnapshot == null ? 0 : currentSnapshot.wordCount() - previousSnapshot.wordCount();
    }

    public boolean formatChanged() {
        return currentSnapshot != null && currentSnapshot.format() != previousSnapshot.format();
    }

    public boolean structureChanged() {
        return currentSnapshot != null && currentSnapshot.blockCount() != previousSnapshot.blockCount();
    }

    public String detailedSummary() {
        if (currentSnapshot == null) {
            return summary;
        }
        return summary + " Bloques: " + previousSnapshot.blockCount() + " → " + currentSnapshot.blockCount()
                + "; palabras: " + previousSnapshot.wordCount() + " → " + currentSnapshot.wordCount()
                + "; formato: " + previousSnapshot.format().displayName() + " → " + currentSnapshot.format().displayName() + ".";
    }

    public boolean hasContentChanges() {
        return status == SourceDocumentChangeStatus.CHANGED;
    }

    public boolean requiresUserReview() {
        return status != SourceDocumentChangeStatus.UNCHANGED
                || layersFreshness == DerivedArtifactFreshness.REVIEW_REQUIRED
                || storyboardFreshness == DerivedArtifactFreshness.REVIEW_REQUIRED;
    }

    public boolean audioShouldBeRegenerated() {
        return audioFreshness == DerivedArtifactFreshness.STALE;
    }

    private static String defaultSummary(SourceDocumentChangeStatus status) {
        return switch (status) {
            case UNCHANGED -> "Sin cambios detectados.";
            case CHANGED -> "Cambios detectados en el documento fuente.";
            case MISSING -> "No se encontró el archivo fuente.";
            case UNSUPPORTED -> "El formato fuente no está soportado para refresco.";
        };
    }
}
