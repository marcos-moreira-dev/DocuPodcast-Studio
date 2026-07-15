package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreBuiltInCameraCatalogTest {
    @Test
    void builtInCatalogProvidesDefaultCameraAndAllBundledReferences() {
        assertEquals(18, TheatreBuiltInCameraCatalog.references().size());
        assertTrue(TheatreBuiltInCameraCatalog.contains(TheatreProjectLayer.DEFAULT_CAMERA_ID));
        assertTrue(TheatreBuiltInCameraCatalog.resourceUri(TheatreProjectLayer.DEFAULT_CAMERA_ID).isPresent());
        assertTrue(TheatreBuiltInCameraCatalog.resourcePath(TheatreProjectLayer.DEFAULT_CAMERA_ID).isPresent());
    }
}
