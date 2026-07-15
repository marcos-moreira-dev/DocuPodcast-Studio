package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import java.util.EnumSet;
import java.util.Set;

/**
 * Product navigation policy for the desktop surface after the GUI contract cleanup.
 *
 * <p>The product now has real workspaces for Inicio, Documento, Visual narrativa, Teatro/Guion,
 * Voces and gestion de frames teatrales.
 * Internal preparation, audio jobs and standalone visual sequencing remain as legacy/internal implementation surfaces
 * while the GUI is refactored, but they must not compete in normal navigation.</p>
 */
public final class WorkspaceSurfacePolicy {
    private static final Set<WorkspaceKind> PRODUCT_SURFACES = EnumSet.of(
            WorkspaceKind.WELCOME_HOME,
            WorkspaceKind.DOCUMENT_READER,
            WorkspaceKind.THEATRE_SCRIPT,
            WorkspaceKind.VOICE_LIBRARY,
            WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION,
            WorkspaceKind.THEATRE_IMAGE_GENERATION
    );

    private static final Set<WorkspaceKind> LEGACY_INTERNAL_SURFACES = EnumSet.of(
            WorkspaceKind.SCRIPT_EDITOR,
            WorkspaceKind.AUDIO_JOBS,
            WorkspaceKind.STORYBOARD
    );

    public boolean isPrimarySurface(WorkspaceKind workspaceKind) {
        return workspaceKind != null && PRODUCT_SURFACES.contains(workspaceKind);
    }

    /** Legacy name kept for existing source tests and policies. */
    public boolean isAdvancedSurface(WorkspaceKind workspaceKind) {
        return isLegacyInternalSurface(workspaceKind);
    }

    /** Explicit product quarantine label: these surfaces may exist in code but are not product navigation. */
    public boolean isLegacyInternalSurface(WorkspaceKind workspaceKind) {
        return workspaceKind != null && LEGACY_INTERNAL_SURFACES.contains(workspaceKind);
    }

    public boolean isVisibleInMainNavigation(WorkspaceKind workspaceKind) {
        return isPrimarySurface(workspaceKind);
    }

    public WorkspaceKind restoreStartupWorkspace(WorkspaceKind persistedWorkspace) {
        if (persistedWorkspace == null) {
            return WorkspaceKind.WELCOME_HOME;
        }
        if (isPrimarySurface(persistedWorkspace)) {
            return persistedWorkspace;
        }
        if (isAdvancedSurface(persistedWorkspace)) {
            return WorkspaceKind.DOCUMENT_READER;
        }
        return WorkspaceKind.WELCOME_HOME;
    }

    public Set<WorkspaceKind> primarySurfaces() {
        return Set.copyOf(PRODUCT_SURFACES);
    }

    public Set<WorkspaceKind> advancedSurfaces() {
        return Set.copyOf(LEGACY_INTERNAL_SURFACES);
    }
}
