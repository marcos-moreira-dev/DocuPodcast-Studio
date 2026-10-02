package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.nio.file.Path;
import java.util.Objects;

/** Resolved primary visual used for theatre rendering and A/B interpolation references. */
public record TheatrePrimaryVisualReference(
        String interventionId,
        String segmentId,
        ProjectAssetReference asset,
        Path absolutePath,
        Source source
) {
    public TheatrePrimaryVisualReference {
        interventionId = clean(interventionId);
        segmentId = clean(segmentId);
        asset = Objects.requireNonNull(asset, "asset");
        absolutePath = Objects.requireNonNull(absolutePath, "absolutePath").toAbsolutePath().normalize();
        source = source == null ? Source.LEGACY_THEATRE_VISUAL : source;
    }

    public String assetId() {
        return asset.id();
    }

    public String relativePath() {
        return asset.relativePath();
    }

    public enum Source {
        OFFICIAL_IMAGE,
        GENERATED_IMAGE,
        STORYBOARD_FRAME,
        SCENERY_COMPOSITION,
        LEGACY_THEATRE_VISUAL
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
