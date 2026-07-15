package com.marcosmoreiradev.docupodcaststudio.application.guide;

import java.util.List;
import java.util.Objects;

/** Executes local, offline search across the integrated guide. */
public final class SearchGuideTopicsUseCase {
    private final GuideCatalog catalog;

    public SearchGuideTopicsUseCase(GuideCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public List<GuideSearchResult> search(String query) {
        return catalog.search(query);
    }
}
