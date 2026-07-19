package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.List;

/** Immutable, semantically labelled references supplied to a visual workflow. */
public record VisualReferenceBundle(List<VisualConditioningReference> references) {
    public VisualReferenceBundle {
        references = references == null
                ? List.of()
                : references.stream().filter(java.util.Objects::nonNull).toList();
    }

    public static VisualReferenceBundle empty() {
        return new VisualReferenceBundle(List.of());
    }

    public List<VisualConditioningReference> byRole(VisualConditioningRole role) {
        return references.stream().filter(reference -> reference.role() == role).toList();
    }

    public boolean has(VisualConditioningRole role) {
        return references.stream().anyMatch(reference -> reference.role() == role);
    }
}
