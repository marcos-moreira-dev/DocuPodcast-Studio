package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import javafx.scene.Node;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Lazily creates and resolves workspace views from official workspace descriptors. */
public final class WorkspaceViewRegistry {
    private final WorkspaceDescriptorCatalog catalog;
    private final WorkspaceSurfacePolicy surfacePolicy = new WorkspaceSurfacePolicy();
    private final Map<WorkspaceKind, Supplier<Node>> factories = new EnumMap<>(WorkspaceKind.class);
    private final Map<WorkspaceKind, Node> cache = new EnumMap<>(WorkspaceKind.class);

    public WorkspaceViewRegistry(WorkspaceDescriptorCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public WorkspaceViewRegistry register(WorkspaceKind kind, Supplier<Node> factory) {
        WorkspaceKind resolved = Objects.requireNonNull(kind, "kind");
        if (!surfacePolicy.isPrimarySurface(resolved)) {
            throw new IllegalArgumentException("Workspace heredado no registrable en la superficie principal: " + resolved);
        }
        factories.put(resolved, Objects.requireNonNull(factory, "factory"));
        return this;
    }

    public Node viewFor(WorkspaceKind kind) {
        WorkspaceKind resolved = surfacePolicy.restoreStartupWorkspace(catalog.descriptor(kind).kind());
        return cache.computeIfAbsent(resolved, this::createView);
    }

    public WorkspaceDescriptor descriptor(WorkspaceKind kind) {
        return catalog.descriptor(kind);
    }

    public boolean hasRegisteredFactory(WorkspaceKind kind) {
        return factories.containsKey(kind);
    }

    private Node createView(WorkspaceKind kind) {
        Supplier<Node> factory = factories.get(kind);
        if (factory != null) {
            return factory.get();
        }
        return new PlaceholderWorkspaceView(kind);
    }
}
