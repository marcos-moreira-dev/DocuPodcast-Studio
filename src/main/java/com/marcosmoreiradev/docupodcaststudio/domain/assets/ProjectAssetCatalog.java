package com.marcosmoreiradev.docupodcaststudio.domain.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Immutable catalog of project assets. */
public final class ProjectAssetCatalog {
    private final List<ProjectAssetReference> references;
    private final Map<String, ProjectAssetReference> byId;

    public ProjectAssetCatalog(List<ProjectAssetReference> references) {
        Objects.requireNonNull(references, "references");
        LinkedHashMap<String, ProjectAssetReference> ordered = new LinkedHashMap<>();
        for (ProjectAssetReference reference : references) {
            Objects.requireNonNull(reference, "reference");
            if (ordered.containsKey(reference.id())) {
                throw new IllegalArgumentException("Duplicated asset id: " + reference.id());
            }
            ordered.put(reference.id(), reference);
        }
        this.byId = Collections.unmodifiableMap(ordered);
        this.references = List.copyOf(ordered.values());
    }

    public static ProjectAssetCatalog empty() {
        return new ProjectAssetCatalog(List.of());
    }

    public List<ProjectAssetReference> references() {
        return references;
    }

    public int size() {
        return references.size();
    }

    public Optional<ProjectAssetReference> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<ProjectAssetReference> byKind(ProjectAssetKind kind) {
        Objects.requireNonNull(kind, "kind");
        return references.stream()
                .filter(reference -> reference.kind() == kind)
                .collect(Collectors.toUnmodifiableList());
    }

    public boolean containsKind(ProjectAssetKind kind) {
        return references.stream().anyMatch(reference -> reference.kind() == kind);
    }

    public ProjectAssetCatalog withReference(ProjectAssetReference reference) {
        Objects.requireNonNull(reference, "reference");
        List<ProjectAssetReference> updated = new ArrayList<>(references);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).id().equals(reference.id())) {
                updated.set(i, reference);
                return new ProjectAssetCatalog(updated);
            }
        }
        updated.add(reference);
        return new ProjectAssetCatalog(updated);
    }

    public ProjectAssetCatalog withoutReference(String id) {
        List<ProjectAssetReference> updated = references.stream()
                .filter(reference -> !reference.id().equals(id))
                .toList();
        return new ProjectAssetCatalog(updated);
    }
}
