package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.presentation.compatibility.LegacyWorkspaceRouteMapper;

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
    private final Set<WorkspaceKind> productSurfaces;
    private final LegacyWorkspaceRouteMapper legacyRoutes;

    public WorkspaceSurfacePolicy() {
        this(ProjectExperienceRegistry.official(), new LegacyWorkspaceRouteMapper());
    }

    WorkspaceSurfacePolicy(ProjectExperienceRegistry experiences, LegacyWorkspaceRouteMapper legacyRoutes) {
        EnumSet<WorkspaceKind> surfaces = EnumSet.of(WorkspaceKind.WELCOME_HOME);
        experiences.experiences().forEach(experience -> surfaces.addAll(experience.workspaces()));
        this.productSurfaces = Set.copyOf(surfaces);
        this.legacyRoutes = legacyRoutes;
    }

    public boolean isPrimarySurface(WorkspaceKind workspaceKind) {
        return workspaceKind != null && productSurfaces.contains(workspaceKind);
    }

    /** Legacy name kept for existing source tests and policies. */
    public boolean isAdvancedSurface(WorkspaceKind workspaceKind) {
        return isLegacyInternalSurface(workspaceKind);
    }

    /** Explicit product quarantine label: these surfaces may exist in code but are not product navigation. */
    public boolean isLegacyInternalSurface(WorkspaceKind workspaceKind) {
        return legacyRoutes.isHistoricalInternal(workspaceKind);
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
            return legacyRoutes.productDestination(persistedWorkspace);
        }
        return WorkspaceKind.WELCOME_HOME;
    }

    public Set<WorkspaceKind> primarySurfaces() {
        return productSurfaces;
    }

    public Set<WorkspaceKind> advancedSurfaces() {
        return legacyRoutes.historicalInternalKinds();
    }
}
