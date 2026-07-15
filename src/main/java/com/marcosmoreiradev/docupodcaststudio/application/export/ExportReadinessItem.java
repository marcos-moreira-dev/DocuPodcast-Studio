package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.util.List;
import java.util.Objects;

/** One row in the export readiness matrix: output, status, evidence and honest limitations. */
public record ExportReadinessItem(
        ExportableArtifactKind kind,
        DocuPodcastExportFormat format,
        ExportReadinessStatus status,
        String targetHint,
        List<String> evidence,
        List<String> missingRequirements,
        List<String> limitations
) {
    public ExportReadinessItem {
        kind = Objects.requireNonNull(kind, "kind");
        format = Objects.requireNonNull(format, "format");
        status = Objects.requireNonNull(status, "status");
        targetHint = targetHint == null ? "" : targetHint.strip();
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        missingRequirements = missingRequirements == null ? List.of() : List.copyOf(missingRequirements);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static ExportReadinessItem exportable(
            ExportableArtifactKind kind,
            DocuPodcastExportFormat format,
            String targetHint,
            List<String> evidence,
            List<String> limitations
    ) {
        return new ExportReadinessItem(kind, format, ExportReadinessStatus.EXPORTABLE, targetHint,
                evidence, List.of(), limitations);
    }

    public static ExportReadinessItem warning(
            ExportableArtifactKind kind,
            DocuPodcastExportFormat format,
            String targetHint,
            List<String> evidence,
            List<String> missingRequirements,
            List<String> limitations
    ) {
        return new ExportReadinessItem(kind, format, ExportReadinessStatus.EXPORTABLE_CON_ADVERTENCIAS, targetHint,
                evidence, missingRequirements, limitations);
    }

    public static ExportReadinessItem blocked(
            ExportableArtifactKind kind,
            DocuPodcastExportFormat format,
            String targetHint,
            List<String> missingRequirements,
            List<String> limitations
    ) {
        return new ExportReadinessItem(kind, format, ExportReadinessStatus.BLOQUEADO, targetHint,
                List.of(), missingRequirements, limitations);
    }

    public boolean exportable() {
        return status.exportable();
    }

    public boolean blocked() {
        return status.blocked();
    }
}
