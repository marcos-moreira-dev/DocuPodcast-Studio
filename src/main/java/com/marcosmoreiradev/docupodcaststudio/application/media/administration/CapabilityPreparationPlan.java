package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import java.util.List;

/** Explicit preparation proposal; large downloads remain unselected by default. */
public record CapabilityPreparationPlan(
        CapabilityRequirement requirement,
        List<ManagedComponentDescriptor> components,
        long requiredBytes,
        List<String> downloads,
        List<String> imports,
        boolean explicitConfirmationRequired) {
    public CapabilityPreparationPlan {
        components = components == null ? List.of() : List.copyOf(components);
        downloads = downloads == null ? List.of() : List.copyOf(downloads);
        imports = imports == null ? List.of() : List.copyOf(imports);
        requiredBytes = Math.max(0L, requiredBytes);
    }
}
