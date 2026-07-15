package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.util.List;

/** Diagnostics collected during document import. */
public record DocumentImportReport(List<DocumentImportIssue> issues) {
    public DocumentImportReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public static DocumentImportReport empty() {
        return new DocumentImportReport(List.of());
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public boolean hasWarnings() {
        return issues.stream().anyMatch(issue -> issue.level() == DocumentImportIssueLevel.WARNING);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.level() == DocumentImportIssueLevel.ERROR);
    }

    public long warningCount() {
        return issues.stream().filter(issue -> issue.level() == DocumentImportIssueLevel.WARNING).count();
    }

    public long errorCount() {
        return issues.stream().filter(issue -> issue.level() == DocumentImportIssueLevel.ERROR).count();
    }
}
