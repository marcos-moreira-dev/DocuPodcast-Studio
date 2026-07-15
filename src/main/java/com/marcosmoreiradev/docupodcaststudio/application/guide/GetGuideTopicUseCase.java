package com.marcosmoreiradev.docupodcaststudio.application.guide;

import java.util.Objects;

/** Retrieves one official guide topic by ID. */
public final class GetGuideTopicUseCase {
    private final GuideCatalog catalog;

    public GetGuideTopicUseCase(GuideCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public GuideTopic get(GuideTopicId id) {
        return catalog.topic(id).orElseThrow(() -> new IllegalArgumentException("No guide topic registered for " + id));
    }
}
