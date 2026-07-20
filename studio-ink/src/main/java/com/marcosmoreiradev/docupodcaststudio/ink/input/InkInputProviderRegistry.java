package com.marcosmoreiradev.docupodcaststudio.ink.input;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingFeatureCatalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Explicit input-provider registry built once by desktop composition. */
public final class InkInputProviderRegistry {
    private final Map<String, Supplier<InkInputProvider>> providers = new LinkedHashMap<>();

    public InkInputProviderRegistry register(String drawingProfileId, Supplier<InkInputProvider> provider) {
        String id = drawingProfileId == null ? "" : drawingProfileId.strip();
        if (id.isBlank()) throw new IllegalArgumentException("drawing profile id is required");
        if (providers.putIfAbsent(id, Objects.requireNonNull(provider, "provider")) != null) {
            throw new IllegalArgumentException("input provider already registered: " + id);
        }
        return this;
    }

    public InkInputProvider create(String drawingProfileId) {
        Supplier<InkInputProvider> supplier = providers.get(drawingProfileId);
        if (supplier == null) throw new IllegalArgumentException("input provider not registered: " + drawingProfileId);
        return Objects.requireNonNull(supplier.get(), "input provider supplier returned null");
    }

    public static InkInputProviderRegistry localDefaults() {
        return new InkInputProviderRegistry()
                .register(DrawingFeatureCatalog.DOCUMENT_PROBLEM, InkInputProviderFactory::createLectureStudioOnly)
                .register(DrawingFeatureCatalog.FREE_COMPOSITION, InkInputProviderFactory::createDefault)
                .register(DrawingFeatureCatalog.THEATRE_FRAME, InkInputProviderFactory::createNativeOnly);
    }
}
