package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.UiState;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.UiStateView;
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
    private final Map<WorkspaceKind, WorkspaceKind> aliases = new EnumMap<>(WorkspaceKind.class);
    private final Map<WorkspaceKind, Node> cache = new EnumMap<>(WorkspaceKind.class);

    public WorkspaceViewRegistry(WorkspaceDescriptorCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public WorkspaceViewRegistry register(WorkspaceKind kind, Supplier<Node> factory) {
        WorkspaceKind resolved = Objects.requireNonNull(kind, "kind");
        if (!surfacePolicy.isPrimarySurface(resolved)) {
            throw new IllegalArgumentException("Workspace heredado no registrable en la superficie principal: " + resolved);
        }
        if (factories.putIfAbsent(resolved, Objects.requireNonNull(factory, "factory")) != null
                || aliases.containsKey(resolved)) {
            throw new IllegalArgumentException("Workspace ya registrado: " + resolved);
        }
        return this;
    }

    /** Explicit product delegation, used while Narrative shares the document composition. */
    public WorkspaceViewRegistry registerAlias(WorkspaceKind kind, WorkspaceKind target) {
        WorkspaceKind resolved = Objects.requireNonNull(kind, "kind");
        WorkspaceKind resolvedTarget = Objects.requireNonNull(target, "target");
        if (!surfacePolicy.isPrimarySurface(resolved) || !surfacePolicy.isPrimarySurface(resolvedTarget)) {
            throw new IllegalArgumentException("Solo se pueden delegar superficies principales");
        }
        if (resolved == resolvedTarget || factories.containsKey(resolved)
                || aliases.putIfAbsent(resolved, resolvedTarget) != null) {
            throw new IllegalArgumentException("Delegación inválida o duplicada: " + resolved);
        }
        return this;
    }

    public Node viewFor(WorkspaceKind kind) {
        WorkspaceKind resolved = surfacePolicy.restoreStartupWorkspace(catalog.descriptor(kind).kind());
        return cache.computeIfAbsent(resolved, this::createView);
    }

    public WorkspaceDescriptor descriptor(WorkspaceKind kind) {
        return catalog.descriptor(kind);
    }

    public boolean isCached(WorkspaceKind kind) {
        WorkspaceKind resolved = surfacePolicy.restoreStartupWorkspace(catalog.descriptor(kind).kind());
        return cache.containsKey(resolved);
    }

    public boolean hasRegisteredFactory(WorkspaceKind kind) {
        return factories.containsKey(kind);
    }

    public boolean hasExplicitRegistration(WorkspaceKind kind) {
        return factories.containsKey(kind) || aliases.containsKey(kind);
    }

    public void validateRegistrations() {
        for (WorkspaceDescriptor descriptor : catalog.descriptors().values()) {
            if (descriptor.implemented() && surfacePolicy.isPrimarySurface(descriptor.kind())
                    && !hasExplicitRegistration(descriptor.kind())) {
                throw new IllegalStateException("Workspace implementado sin fábrica ni delegación: " + descriptor.kind());
            }
        }
        for (Map.Entry<WorkspaceKind, WorkspaceKind> alias : aliases.entrySet()) {
            if (!factories.containsKey(alias.getValue())) {
                throw new IllegalStateException("Delegación sin fábrica destino: " + alias.getKey()
                        + " -> " + alias.getValue());
            }
        }
    }

    private Node createView(WorkspaceKind kind) {
        Supplier<Node> factory = factories.get(kind);
        if (factory != null) {
            return Objects.requireNonNull(factory.get(), "workspace factory returned null: " + kind);
        }
        WorkspaceKind target = aliases.get(kind);
        if (target != null) return viewFor(target);
        WorkspaceDescriptor descriptor = catalog.descriptor(kind);
        if (descriptor.implemented()) {
            throw new IllegalStateException("Workspace implementado sin fábrica ni delegación: " + kind);
        }
        return new UiStateView(UiState.CAPABILITY_UNAVAILABLE, "NO DISPONIBLE", descriptor.title(),
                "Esta capacidad no forma parte de la superficie operativa actual.");
    }
}
