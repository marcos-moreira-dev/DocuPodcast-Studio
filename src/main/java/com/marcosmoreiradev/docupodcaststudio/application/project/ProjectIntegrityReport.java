package com.marcosmoreiradev.docupodcaststudio.application.project;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Non-binary brain-side integrity report for a DocuPodcast project.
 *
 * <p>The report is intentionally independent from JavaFX. It separates recoverable warnings
 * from blocking findings so opening a project can distinguish "usable with warnings" from
 * "requires repair before trusting export/playback".</p>
 */
public record ProjectIntegrityReport(
        String projectTitle,
        ProjectIntegrityStatus status,
        List<ProjectIntegrityIssue> issues,
        Instant checkedAt
) {
    public ProjectIntegrityReport {
        projectTitle = normalize(projectTitle).isBlank() ? "Proyecto DocuPodcast" : normalize(projectTitle);
        issues = issues == null ? List.of() : List.copyOf(issues);
        status = status == null ? statusFrom(issues) : status;
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }

    public static ProjectIntegrityReport from(String projectTitle, List<ProjectIntegrityIssue> issues) {
        return new ProjectIntegrityReport(projectTitle, statusFrom(issues), issues, Instant.now());
    }

    public boolean ok() {
        return status == ProjectIntegrityStatus.OK;
    }

    public boolean withWarnings() {
        return status == ProjectIntegrityStatus.CON_ADVERTENCIAS;
    }

    public boolean requiresRepair() {
        return status == ProjectIntegrityStatus.REQUIERE_REPARACION;
    }

    public int errorCount() {
        return (int) issues.stream().filter(issue -> issue.severity() == ProjectIntegritySeverity.ERROR).count();
    }

    public int warningCount() {
        return (int) issues.stream().filter(issue -> issue.severity() == ProjectIntegritySeverity.WARNING).count();
    }

    public int infoCount() {
        return (int) issues.stream().filter(issue -> issue.severity() == ProjectIntegritySeverity.INFO).count();
    }

    public List<String> messages() {
        return issues.stream().map(ProjectIntegrityIssue::displayLine).toList();
    }

    public String summary() {
        if (ok()) {
            return "Integridad OK: proyecto listo para continuar.";
        }
        if (requiresRepair()) {
            return "Integridad requiere reparación: " + errorCount() + " error(es), " + warningCount() + " advertencia(s).";
        }
        return "Integridad con advertencias: " + warningCount() + " advertencia(s).";
    }

    public String toMarkdown() {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Integridad del proyecto DocuPodcast\n\n");
        markdown.append("- Proyecto: ").append(projectTitle).append("\n");
        markdown.append("- Estado: ").append(status.displayName()).append("\n");
        markdown.append("- Fecha UTC: ").append(checkedAt).append("\n");
        markdown.append("- Errores: ").append(errorCount()).append("\n");
        markdown.append("- Advertencias: ").append(warningCount()).append("\n");
        markdown.append("- Infos: ").append(infoCount()).append("\n\n");
        markdown.append("## Hallazgos\n\n");
        if (issues.isEmpty()) {
            markdown.append("- Sin hallazgos: el proyecto está íntegro.\n");
        } else {
            for (ProjectIntegrityIssue issue : issues) {
                markdown.append("- ").append(issue.displayLine()).append("\n");
            }
        }
        markdown.append("\n## Recomendación\n\n");
        markdown.append(summary()).append("\n");
        return markdown.toString();
    }

    private static ProjectIntegrityStatus statusFrom(List<ProjectIntegrityIssue> issues) {
        List<ProjectIntegrityIssue> safeIssues = issues == null ? List.of() : issues;
        if (safeIssues.stream().anyMatch(ProjectIntegrityIssue::blocking)) {
            return ProjectIntegrityStatus.REQUIERE_REPARACION;
        }
        if (safeIssues.stream().anyMatch(issue -> issue.severity() == ProjectIntegritySeverity.WARNING)) {
            return ProjectIntegrityStatus.CON_ADVERTENCIAS;
        }
        return ProjectIntegrityStatus.OK;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
