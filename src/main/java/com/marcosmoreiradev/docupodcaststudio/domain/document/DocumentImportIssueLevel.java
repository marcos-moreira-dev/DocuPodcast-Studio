package com.marcosmoreiradev.docupodcaststudio.domain.document;

/** Severity for issues detected while importing a source document. */
public enum DocumentImportIssueLevel {
    INFO("Info"),
    WARNING("Advertencia"),
    ERROR("Error");

    private final String displayName;

    DocumentImportIssueLevel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
