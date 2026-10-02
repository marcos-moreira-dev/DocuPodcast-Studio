package com.marcosmoreiradev.docupodcaststudio.ink;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProviderFactory;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProviderRegistry;

/** Immutable pair built once by the launcher and injected into desktop. */
public record StudioInkPlatform(DrawingFeatureCatalog drawingFeatures, InkInputProviderRegistry inputProviders) {
    public StudioInkPlatform {
        if (drawingFeatures == null || inputProviders == null) throw new IllegalArgumentException("complete ink platform is required");
    }

    public static StudioInkPlatform local() {
        DrawingFeatureCatalog catalog = DrawingFeatureCatalog.official();
        InkInputProviderRegistry providers = new InkInputProviderRegistry();
        catalog.profiles().forEach(profile -> providers.register(profile.id(),
                () -> InkInputProviderFactory.create(profile.inputPolicy())));
        return new StudioInkPlatform(catalog, providers);
    }
}
