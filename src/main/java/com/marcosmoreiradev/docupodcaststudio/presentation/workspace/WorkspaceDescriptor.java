package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Metadata contract for a DocuPodcast workspace. */
public record WorkspaceDescriptor(
        WorkspaceKind kind,
        String title,
        String navigationLabel,
        String description,
        boolean implemented,
        boolean primaryNavigation,
        Set<WorkspaceCapability> capabilities
) {
    public WorkspaceDescriptor {
        Objects.requireNonNull(kind, "kind");
        title = requireText(title, "title");
        navigationLabel = requireText(navigationLabel, "navigationLabel");
        description = requireText(description, "description");
        capabilities = capabilities == null || capabilities.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    public boolean supports(WorkspaceCapability capability) {
        return capabilities.contains(capability);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacio.");
        }
        return value.strip();
    }
}
