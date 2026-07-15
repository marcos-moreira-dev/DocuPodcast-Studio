package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;

import java.util.stream.Collectors;

/** Converts project integrity findings into a user-facing decision for the desktop shell. */
public final class ProjectIntegrityUserDecisionFactory {
    private ProjectIntegrityUserDecisionFactory() {
    }

    public static UserVisibleDecision fromReport(ProjectIntegrityReport report) {
        if (report == null || report.ok()) {
            return UserVisibleDecision.information("Proyecto íntegro", "El proyecto no tiene advertencias de integridad.");
        }
        String findings = report.issues().stream()
                .limit(8)
                .map(ProjectIntegrityIssue::displayLine)
                .collect(Collectors.joining(System.lineSeparator()));
        if (report.issues().size() > 8) {
            findings += System.lineSeparator() + "… y " + (report.issues().size() - 8) + " hallazgo(s) adicional(es).";
        }
        String message = report.requiresRepair()
                ? "El proyecto se abrió, pero tiene archivos o referencias que requieren reparación antes de confiar en exportación, audio o video."
                : "El proyecto se abrió con advertencias. Puedes continuar, pero revisa estos puntos antes de exportar o renderizar.";
        String headline = report.requiresRepair() ? "Proyecto requiere reparación" : "Proyecto abierto con advertencias";
        return new UserVisibleDecision(
                report.requiresRepair()
                        ? com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity.ERROR
                        : com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity.WARNING,
                "Integridad del proyecto",
                headline,
                message,
                report.summary() + System.lineSeparator() + findings,
                true
        );
    }
}
