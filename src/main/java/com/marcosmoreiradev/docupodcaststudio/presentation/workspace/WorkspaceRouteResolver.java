package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import java.util.Objects;

/** Resolves requested workspace routes to a safe product workspace descriptor. */
public final class WorkspaceRouteResolver {
    private final WorkspaceDescriptorCatalog catalog;
    private final WorkspaceSurfacePolicy surfacePolicy = new WorkspaceSurfacePolicy();

    public WorkspaceRouteResolver(WorkspaceDescriptorCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public WorkspaceKind resolve(WorkspaceKind requested) {
        if (requested == null || !catalog.isKnown(requested)) {
            return WorkspaceKind.WELCOME_HOME;
        }
        return surfacePolicy.restoreStartupWorkspace(requested);
    }

    public WorkspaceKind resolvePersisted(String persistedName) {
        if (persistedName == null || persistedName.isBlank()) {
            return WorkspaceKind.WELCOME_HOME;
        }
        try {
            return resolve(WorkspaceKind.valueOf(persistedName.strip()));
        } catch (IllegalArgumentException ex) {
            return WorkspaceKind.WELCOME_HOME;
        }
    }
}
