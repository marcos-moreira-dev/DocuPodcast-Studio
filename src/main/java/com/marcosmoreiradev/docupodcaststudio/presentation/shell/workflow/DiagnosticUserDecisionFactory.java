package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;

import java.nio.file.Path;

/** Builds operational UI decisions from diagnostics so reports do not disappear in the status bar. */
public final class DiagnosticUserDecisionFactory {
    private DiagnosticUserDecisionFactory() {
    }

    public static UserVisibleDecision diagnosticReportExported(Path targetFile) {
        String path = targetFile == null ? "reporte diagnóstico" : targetFile.toAbsolutePath().normalize().toString();
        return new UserVisibleDecision(
                DecisionSeverity.INFORMATION,
                "DocuPodcast Studio",
                "Reporte diagnóstico exportado",
                "Se creó el reporte para revisar estado técnico, motores, audio, visuales y exportación. Archivo: " + path,
                "Reporte Markdown: " + path,
                true);
    }

    public static UserVisibleDecision diagnosticReportNeedsAttention(String headline, String detail) {
        return UserVisibleDecision.warning(
                headline == null || headline.isBlank() ? "Diagnóstico requiere revisión" : headline,
                detail == null || detail.isBlank()
                        ? "Revisa el reporte antes de continuar con exportación o motores."
                        : detail.strip());
    }
}
