package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DerivedArtifactFreshness;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentChangeStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Builds user-facing refresh decisions without leaking technical scaffolding into the UI. */
public final class SourceDocumentRefreshDecisionFactory {
    private SourceDocumentRefreshDecisionFactory() {
    }

    public static Optional<UserVisibleDecision> fromReport(SourceDocumentChangeReport report, boolean projectContentChanged) {
        Objects.requireNonNull(report, "report");
        if (report.status() == SourceDocumentChangeStatus.UNCHANGED && !projectContentChanged) {
            return Optional.empty();
        }
        if (report.status() == SourceDocumentChangeStatus.MISSING || report.status() == SourceDocumentChangeStatus.UNSUPPORTED) {
            return Optional.of(UserVisibleDecision.error(
                    "No se pudo refrescar la fuente documental",
                    report.summary(),
                    report.detailedSummary()));
        }
        if (report.status() == SourceDocumentChangeStatus.CHANGED) {
            return Optional.of(UserVisibleDecision.defensiveFallback(
                    "El documento fuente cambió",
                    "DocuPodcast refrescó la lectura, pero el audio queda obsoleto y las capas visuales/de voz requieren revisión antes de exportar.",
                    detail(report)));
        }
        if (projectContentChanged || report.layersFreshness() == DerivedArtifactFreshness.REVIEW_REQUIRED
                || report.storyboardFreshness() == DerivedArtifactFreshness.REVIEW_REQUIRED) {
            return Optional.of(UserVisibleDecision.warning(
                    "El documento necesita revisión",
                    "Se actualizaron datos internos del documento. Revisa visuales, voces o audio asociados antes de exportar."));
        }
        return Optional.empty();
    }

    private static String detail(SourceDocumentChangeReport report) {
        List<String> lines = new ArrayList<>();
        lines.add(report.detailedSummary());
        lines.add("Audio: " + report.audioFreshness().displayName());
        lines.add("Capas: " + report.layersFreshness().displayName());
        lines.add("Visuales: " + report.storyboardFreshness().displayName());
        return String.join(System.lineSeparator(), lines);
    }
}
