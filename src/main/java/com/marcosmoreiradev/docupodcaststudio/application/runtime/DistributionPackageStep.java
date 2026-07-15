package com.marcosmoreiradev.docupodcaststudio.application.runtime;

/** One auditable step in a distribution package plan. */
public record DistributionPackageStep(String id, String title, String command, String evidencePath, boolean mandatory) {
    public DistributionPackageStep {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        command = command == null ? "" : command.trim();
        evidencePath = evidencePath == null ? "" : evidencePath.trim();
    }
}
