package com.marcosmoreiradev.docupodcaststudio.ink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class StudioInkPlatformFallbackTest {
    @Test
    void everyProductDrawingProfileHasAnUsableInputProviderWithoutOptionalHardware() {
        StudioInkPlatform platform = StudioInkPlatform.local();

        for (String profileId : java.util.List.of(
                DrawingFeatureCatalog.DOCUMENT_PROBLEM,
                DrawingFeatureCatalog.FREE_COMPOSITION,
                DrawingFeatureCatalog.DOCUMENTARY_ILLUSTRATION,
                DrawingFeatureCatalog.THEATRE_FRAME)) {
            var provider = assertDoesNotThrow(() -> platform.inputProviders().create(profileId));
            assertFalse(provider.capabilities().providerName().isBlank());
            assertDoesNotThrow(provider::detach);
        }
    }
}
