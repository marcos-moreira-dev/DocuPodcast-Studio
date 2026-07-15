package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;
import java.util.Objects;

/** Packaging plan for app-image/MSI builds with runtime and legal evidence. */
public record DistributionPackagePlan(DistributionPackageKind kind, List<DistributionPackageStep> steps) {
    public DistributionPackagePlan {
        Objects.requireNonNull(kind, "kind");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    }

    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# DocuPodcast Studio - Distribution package plan\n\n");
        builder.append("Kind: `").append(kind).append("`\n\n");
        builder.append("| Step | Command | Evidence | Mandatory |\n");
        builder.append("|---|---|---|---:|\n");
        for (DistributionPackageStep step : steps) {
            builder.append("| ").append(step.title())
                    .append(" | `").append(step.command()).append("`")
                    .append(" | `").append(step.evidencePath()).append("`")
                    .append(" | ").append(step.mandatory() ? "yes" : "no")
                    .append(" |\n");
        }
        return builder.toString();
    }
}
