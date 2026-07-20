package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Set;

/** Atomic set of resources required by one job. */
public record ResourceRequirement(Set<ResourceId> resources) {
    public static final ResourceRequirement NONE = new ResourceRequirement(Set.of());

    public ResourceRequirement {
        resources = resources == null ? Set.of() : Set.copyOf(resources);
    }

    public static ResourceRequirement of(ResourceId... resources) {
        return new ResourceRequirement(resources == null ? Set.of() : Set.of(resources));
    }
}
