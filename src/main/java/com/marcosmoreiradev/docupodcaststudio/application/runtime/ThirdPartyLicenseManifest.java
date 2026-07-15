package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;
import java.util.Objects;

/** Legal manifest for tools, models, scripts and demo assets considered for distribution. */
public record ThirdPartyLicenseManifest(List<ThirdPartyComponent> components) {
    public ThirdPartyLicenseManifest {
        components = List.copyOf(Objects.requireNonNull(components, "components"));
    }

    public List<ThirdPartyComponent> redistributedByDefault() {
        return components.stream().filter(ThirdPartyComponent::redistributedByDefault).toList();
    }

    public List<ThirdPartyComponent> userProvidedOrPending() {
        return components.stream().filter(ThirdPartyComponent::requiresUserProvidedFiles).toList();
    }

    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# DocuPodcast Studio - Third-party manifest\n\n");
        builder.append("This manifest is an audit aid. It does not grant redistribution rights by itself.\n\n");
        builder.append("| ID | Component | Kind | Location | License | Redistributed by default | User provided | Notes |\n");
        builder.append("|---|---|---|---|---|---:|---:|---|\n");
        for (ThirdPartyComponent component : components) {
            builder.append("| ").append(component.id())
                    .append(" | ").append(escape(component.displayName()))
                    .append(" | ").append(component.kind())
                    .append(" | `").append(component.expectedLocation()).append("`")
                    .append(" | ").append(escape(component.licenseName()))
                    .append(" | ").append(component.redistributedByDefault() ? "yes" : "no")
                    .append(" | ").append(component.requiresUserProvidedFiles() ? "yes" : "no")
                    .append(" | ").append(escape(component.notes()))
                    .append(" |\n");
        }
        return builder.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|");
    }
}
