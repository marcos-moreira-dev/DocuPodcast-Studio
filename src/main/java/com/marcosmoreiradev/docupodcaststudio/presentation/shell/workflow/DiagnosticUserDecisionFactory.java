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
                "Paquete de soporte exportado",
                "Se creó un ZIP local sanitizado con estado técnico y logs recientes. No se subió ningún archivo. Archivo: " + path,
                "Paquete ZIP sanitizado: " + path,
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
